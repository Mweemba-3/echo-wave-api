from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import yt_dlp
import os
import uuid
import threading
import time
import tempfile

app = Flask(__name__)
CORS(app)

# Simple cache - no expiry limits
stream_cache = {}
search_cache = {}
DOWNLOAD_FOLDER = tempfile.gettempdir() + "/echo_wave_downloads"
os.makedirs(DOWNLOAD_FOLDER, exist_ok=True)

def extract_video_id(url):
    if 'youtube.com/watch?v=' in url:
        return url.split('v=')[1].split('&')[0]
    elif 'youtu.be/' in url:
        return url.split('/')[-1].split('?')[0]
    return None

@app.route('/')
def home():
    return jsonify({
        'service': 'Echo-Wave Music API',
        'status': 'running',
        'endpoints': {
            'search': '/api/search?q=query&limit=20',
            'stream': '/api/stream?url=YOUTUBE_URL',
            'download': '/api/download?url=YOUTUBE_URL',
            'health': '/api/health'
        }
    })

@app.route('/api/health')
def health():
    return jsonify({
        'status': 'ok',
        'cache_size': len(stream_cache),
        'active_downloads': len(os.listdir(DOWNLOAD_FOLDER))
    })

@app.route('/api/search')
def search():
    """Search - No limits, no rate limiting"""
    try:
        query = request.args.get('q', '')
        limit = min(int(request.args.get('limit', 50)), 200)  # Max 200 results
        
        if not query:
            return jsonify({'error': 'No search query'}), 400
        
        # Check cache
        cache_key = f"search:{query}:{limit}"
        if cache_key in search_cache:
            return jsonify(search_cache[cache_key])
        
        ydl_opts = {
            'quiet': True,
            'extract_flat': True,
            'no_warnings': True,
            'ignoreerrors': True,
        }
        
        results = []
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            search_query = f"ytsearch{limit}:{query}"
            info = ydl.extract_info(search_query, download=False)
            
            if 'entries' in info:
                for entry in info['entries']:
                    if entry and entry.get('title'):
                        video_id = entry.get('id', '')
                        results.append({
                            'id': video_id,
                            'title': entry.get('title', 'Unknown'),
                            'artist': entry.get('uploader', 'Unknown Artist'),
                            'duration': entry.get('duration', 0),
                            'duration_formatted': format_duration(entry.get('duration', 0)),
                            'url': f"https://www.youtube.com/watch?v={video_id}",
                            'thumbnail': f"https://img.youtube.com/vi/{video_id}/hqdefault.jpg"
                        })
        
        # Cache results (no expiry - stays forever)
        search_cache[cache_key] = results
        return jsonify(results)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/stream')
def stream():
    """Get direct audio URL - No limits"""
    try:
        url = request.args.get('url', '')
        
        if not url:
            return jsonify({'error': 'No URL provided'}), 400
        
        # Check cache
        if url in stream_cache:
            return jsonify(stream_cache[url])
        
        video_id = extract_video_id(url)
        if not video_id:
            return jsonify({'error': 'Invalid YouTube URL'}), 400
        
        ydl_opts = {
            'format': 'bestaudio/best',
            'quiet': True,
            'no_warnings': True,
            'extract_flat': False,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
            # Get best audio URL
            audio_url = None
            if 'url' in info:
                audio_url = info['url']
            elif 'formats' in info:
                for f in info['formats']:
                    if f.get('acodec') != 'none' and f.get('vcodec') == 'none':
                        audio_url = f.get('url')
                        break
                if not audio_url and info['formats']:
                    audio_url = info['formats'][0].get('url')
            
            if not audio_url:
                return jsonify({'error': 'Could not extract audio stream'}), 500
            
            result = {
                'success': True,
                'id': video_id,
                'title': info.get('title', 'Unknown'),
                'artist': info.get('uploader', 'Unknown Artist'),
                'duration': info.get('duration', 0),
                'duration_formatted': format_duration(info.get('duration', 0)),
                'stream_url': audio_url,
                'thumbnail': f"https://img.youtube.com/vi/{video_id}/hqdefault.jpg"
            }
            
            # Cache forever
            stream_cache[url] = result
            return jsonify(result)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/download')
def download():
    """Download MP3 - No limits"""
    try:
        url = request.args.get('url', '')
        
        if not url:
            return jsonify({'error': 'No URL provided'}), 400
        
        video_id = extract_video_id(url)
        if not video_id:
            return jsonify({'error': 'Invalid YouTube URL'}), 400
        
        # Generate unique filename
        filename = f"{video_id}_{uuid.uuid4().hex[:8]}.mp3"
        filepath = os.path.join(DOWNLOAD_FOLDER, filename)
        
        ydl_opts = {
            'format': 'bestaudio/best',
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': 'mp3',
                'preferredquality': '192',
            }],
            'outtmpl': filepath.replace('.mp3', '.%(ext)s'),
            'quiet': True,
            'no_warnings': True,
            'noplaylist': True,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            
            # Find the MP3 file
            mp3_path = filepath
            if not os.path.exists(mp3_path):
                for f in os.listdir(DOWNLOAD_FOLDER):
                    if f.startswith(video_id) and f.endswith('.mp3'):
                        mp3_path = os.path.join(DOWNLOAD_FOLDER, f)
                        break
            
            if os.path.exists(mp3_path):
                # Keep file for 7 days instead of 1 hour
                threading.Timer(7 * 24 * 3600, lambda: os.remove(mp3_path) if os.path.exists(mp3_path) else None).start()
                
                return send_file(
                    mp3_path,
                    as_attachment=True,
                    download_name=f"{info.get('title', 'song')}.mp3",
                    mimetype='audio/mpeg'
                )
            else:
                return jsonify({'error': 'Download failed'}), 500
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

def format_duration(seconds):
    if not seconds:
        return "00:00"
    minutes = int(seconds) // 60
    secs = int(seconds) % 60
    return f"{minutes:02d}:{secs:02d}"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=False)