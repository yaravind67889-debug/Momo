# Momo AI Dedicated Backend

A resilient FastAPI backend for Momo AI orchestration, durable task queue, memory vault, and Hugging Face browser communication.

## Quick Start

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
2. Configure environment:
   ```bash
   cp .env.example .env
   # Edit .env with your HF_SPACE_URL and HF_API_TOKEN
   ```
3. Run the server:
   ```bash
   uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload
   ```
4. API docs are available at `http://localhost:8000/docs`.
