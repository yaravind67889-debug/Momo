import os
import base64
import asyncio
from typing import Dict, Any, Optional
from fastapi import FastAPI, HTTPException, Header, Depends
from pydantic import BaseModel
from playwright.async_api import async_playwright, Browser, Page

app = FastAPI(title="Momo AI — Hugging Face Browser Service")

AUTH_TOKEN = os.getenv("HF_API_TOKEN", "momo_secret_token_123")

class BrowserActionRequest(BaseModel):
    action: str
    params: Dict[str, Any] = {}

class BrowserActionResponse(BaseModel):
    url: str
    title: str
    textSnippet: str
    screenshot: Optional[str] = None

# Global browser lifecycle
_playwright = None
_browser: Optional[Browser] = None
_page: Optional[Page] = None
_lock = asyncio.Lock()

async def get_page() -> Page:
    global _playwright, _browser, _page
    if _page is None or _page.is_closed():
        if _playwright is None:
            _playwright = await async_playwright().start()
        if _browser is None or not _browser.is_connected():
            _browser = await _playwright.chromium.launch(
                headless=True,
                args=["--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"]
            )
        context = await _browser.new_context(viewport={"width": 1280, "height": 800})
        _page = await context.new_page()
    return _page

def verify_token(authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing Authorization Header")
    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(status_code=401, detail="Invalid Authorization Scheme")
    if parts[1] != AUTH_TOKEN:
        raise HTTPException(status_code=403, detail="Invalid Bearer Token")
    return parts[1]

@app.get("/api/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "momo-hf-browser",
        "engine": "Playwright Chromium"
    }

@app.post("/api/browser/execute", response_model=BrowserActionResponse)
async def execute_action(req: BrowserActionRequest, token: str = Depends(verify_token)):
    async with _lock:
        page = await get_page()
        action = req.action
        params = req.params

        try:
            if action == "browser_open":
                url = params.get("url", "https://google.com")
                await page.goto(url, wait_until="domcontentloaded", timeout=30000)
            elif action == "browser_click":
                selector = params.get("selector", "button")
                await page.click(selector, timeout=10000)
            elif action == "browser_type":
                selector = params.get("selector", "input")
                text = params.get("text", "")
                await page.fill(selector, text, timeout=10000)
            elif action == "browser_scroll":
                await page.evaluate("window.scrollBy(0, 500)")
            elif action == "browser_screenshot":
                pass
            elif action == "browser_read_page":
                pass
            else:
                raise HTTPException(status_code=400, detail=f"Unsupported action: {action}")

            current_url = page.url
            title = await page.title()
            
            # Extract readable text
            text_snippet = await page.evaluate("() => document.body.innerText.slice(0, 2000)")
            
            # Capture screenshot
            screenshot_bytes = await page.screenshot(type="jpeg", quality=60)
            screenshot_b64 = base64.b64encode(screenshot_bytes).decode("utf-8")

            return BrowserActionResponse(
                url=current_url,
                title=title,
                textSnippet=text_snippet,
                screenshot=screenshot_b64
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Browser action failed: {str(e)}")

@app.on_event("shutdown")
async def shutdown_event():
    global _browser, _playwright
    if _browser:
        await _browser.close()
    if _playwright:
        await _playwright.stop()
