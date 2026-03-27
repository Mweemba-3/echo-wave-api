FROM python:3.11-slim

# Install ffmpeg
RUN apt-get update && apt-get install -y \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# UNLIMITED - More workers
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--workers", "4", "--threads", "8", "--timeout", "300", "app:app"]