from flask import Flask, request, jsonify
from flask_cors import CORS
import yt_dlp
import os

app = Flask(__name__)
CORS(app)

@app.route('/')
def home():
    return jsonify({
        'service': 'Echo-Wave Music API',
        'status': 'running',
        'endpoints': {
            'search': '/api/search?q=query&limit=20',
            'health': '/api/health'
        }
    })

@app.route('/api/health')
def health():
    return jsonify({
        'status': 'ok',
        'message': 'Echo-Wave API is running!'
    })

@app.route('/api/search')
def search():
    try:
        query = request.args.get('q', '')
        limit = int(request.args.get('limit', 20))
        
        if not query:
            return jsonify({'error': 'No search query'}), 400
        
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
                        duration_raw = entry.get('duration', 0)
                        minutes = int(duration_raw) // 60 if duration_raw else 0
                        seconds = int(duration_raw) % 60 if duration_raw else 0
                        
                        results.append({
                            'id': entry.get('id', ''),
                            'title': entry.get('title', 'Unknown'),
                            'artist': entry.get('uploader', 'Unknown'),
                            'duration': f"{minutes:02d}:{seconds:02d}",
                            'duration_seconds': duration_raw,
                            'url': f"https://www.youtube.com/watch?v={entry.get('id')}",
                            'thumbnail': f"https://img.youtube.com/vi/{entry.get('id')}/hqdefault.jpg"
                        })
        
        return jsonify(results)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=10000)
