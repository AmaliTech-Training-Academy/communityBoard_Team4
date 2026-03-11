FROM python:3.11-slim

WORKDIR /app

# Install dependencies first (cached layer)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy SQL files
COPY database/ database/

# Copy Python source
COPY etl_pipeline.py .

CMD ["python", "etl_pipeline.py"]