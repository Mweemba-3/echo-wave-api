from flask import Flask, request, jsonify
from flask_cors import CORS
import yt_dlp
import os
import re
import uuid

app = Flask(__name__)
CORS(app)

# Create downloads folder
DOWNLOAD_FOLDER = '/tmp/downloads'
os.makedirs(DOWNLOAD_FOLDER, exist_ok=True)

class MusicFetcher:
    def search_youtube(self, query, max_results=20):
        ydl_opts = {
            'quiet': True,
            'extract_flat': True,
            'no_warnings': True,
            'ignoreerrors': True,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                search_query = f"ytsearch{max_results}:{query}"
                info = ydl.extract_info(search_query, download=False)
                results = []
                
                if 'entries' in info:
                    for entry in info['entries']:
                        if entry and entry.get('title'):
                            duration_raw = entry.get('duration')
                            minutes = int(duration_raw) // 60 if duration_raw else 0
                            seconds = int(duration_raw) % 60 if duration_raw else 0
                            
                            results.append({
                                'id': entry.get('id', 'unknown'),
                                'title': entry.get('title', 'Unknown'),
                                'artist': entry.get('uploader', 'Unknown Artist'),
                                'duration': f"{minutes:02d}:{seconds:02d}",
                                'duration_seconds': int(duration_raw) if duration_raw else 0,
                                'url': f"https://www.youtube.com/watch?v={entry.get('id')}",
                                'thumbnail': entry.get('thumbnail', ''),
                                'downloaded': False
                            })
                
                return results
                
            except Exception as e:
                print(f"Search error: {e}")
                return []
    
    def download_audio(self, url, title="", artist=""):
        safe_title = re.sub(r'[<>:"/\\|?*]', '_', title)
        filename = f"{safe_title}_{uuid.uuid4().hex[:8]}.mp3"
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
            'noplaylist': True,
            'no_warnings': True,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                info = ydl.extract_info(url, download=True)
                actual_file = filepath.replace('.mp3', '.mp3')
                
                # Check if file exists
                if os.path.exists(actual_file):
                    return {
                        'id': info.get('id', ''),
                        'title': title,
                        'artist': artist,
                        'filepath': actual_file,
                        'size': os.path.getsize(actual_file)
                    }
                return None
            except Exception as e:
                print(f"Download error: {e}")
                return None

fetcher = MusicFetcher()

@app.route('/')
def home():
    return jsonify({
        'service': 'Echo-Wave Music API',
        'status': 'running',
        'endpoints': {
            'search': '/api/search?q=query&limit=20',
            'download': '/api/download (POST)',
            'health': '/api/health'
        }
    })

@app.route('/api/health')
def health():
    return jsonify({
        'status': 'ok',
        'message': 'Echo-Wave Music API is running on Render!'
    })

@app.route('/api/search')
def search():
    try:
        query = request.args.get('q', '')
        limit = int(request.args.get('limit', 20))
        
        if not query:
            return jsonify({'error': 'No search query'}), 400
        
        results = fetcher.search_youtube(query, limit)
        return jsonify(results)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/download', methods=['POST'])
def download():
    try:
        data = request.get_json()
        url = data.get('url')
        title = data.get('title', '')
        artist = data.get('artist', '')
        
        if not url:
            return jsonify({'error': 'No URL provided'}), 400
        
        result = fetcher.download_audio(url, title, artist)
        
        if result:
            return jsonify({
                'success': True,
                'id': result['id'],
                'title': result['title'],
                'artist': result['artist'],
                'filepath': result['filepath'],
                'size': result['size']
            })
        else:
            return jsonify({'error': 'Download failed'}), 500
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=10000)
