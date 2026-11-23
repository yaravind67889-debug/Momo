# Hugging Face Browser Space for Momo AI

This private Hugging Face Space hosts an authenticated headless Chromium browser controlled via Playwright and FastAPI.

## Deployment Steps

1. Create a new Space on [Hugging Face](https://huggingface.co/new-space):
   - **Space type**: Docker
   - **Visibility**: Private
2. Add your Secret Environment Variable in Space Settings:
   - `HF_API_TOKEN`: Your secret token (e.g. `momo_super_secret_token_xyz`)
3. Upload the files in this directory:
   - `Dockerfile`
   - `requirements.txt`
   - `app.py`
4. Once built and running, copy your Space direct URL:
   - Example: `https://<your-username>-<space-name>.hf.space`
5. In the Momo AI Android app, open the **Settings** tab:
   - Paste the Space URL and Token.
   - Tap **Save & Connect**, then **Test Health**.
