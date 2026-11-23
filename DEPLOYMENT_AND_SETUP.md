# Momo AI — Setup, Deployment & Operations Guide

> **Tagline:** Your little AI, ready to help.  
> **Target Devices:** Android phones, tablets, and desktop browsers.

---

## 1. System Architecture Overview

```
[ User (Android App / Web) ]
             │
             ▼
   [ Momo AI Orchestrator ]
  (Room DB / Durable Tasks / Memory Vault)
             │
   ┌─────────┼──────────────────────────┬────────────────────────┐
   ▼         ▼                          ▼                        ▼
[AI Model] [HF Playwright Space]   [Google Drive]          [Persistent DB]
(Gemini/   (Docker Chromium        (OAuth 2.0 Task         (Tasks, Steps,
 OpenAI)   Browser Automation)      Artifact Storage)       Memories, Logs)
```

---

## 2. Hugging Face Browser Space Deployment

1. **Sign Up / Log In**: Go to [Hugging Face](https://huggingface.co).
2. **Create New Space**:
   - Navigate to **Spaces** → **Create new Space**.
   - **Name**: `momo-browser-space`
   - **Space SDK**: Select **Docker** (Blank).
   - **Visibility**: **Private** (recommended for browser automation safety).
3. **Set Secrets**:
   - Go to Space **Settings** → **Variables and secrets**.
   - Add a Secret: `HF_API_TOKEN` = `<your-custom-secret-passphrase>`
4. **Deploy Files**:
   - Commit and push files from the `/hf_browser_space` directory:
     - `Dockerfile`
     - `requirements.txt`
     - `app.py`
     - `README.md`
5. **Get Space URL**:
   - Once the build succeeds, copy your Space direct URL:
     `https://<your-username>-momo-browser-space.hf.space`
6. **Connect to Momo**:
   - In the Momo Android App, navigate to **Settings** → **Hugging Face Browser Space**.
   - Paste your URL and Bearer Token, tap **Save & Connect**, and click **Test Health**.

---

## 3. Google Drive OAuth 2.0 Setup

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project named `Momo-AI-Agent`.
3. Enable **Google Drive API**:
   - Go to **APIs & Services** → **Library** → Search "Google Drive API" → **Enable**.
4. Configure **OAuth Consent Screen**:
   - User Type: **External** (or Internal for Workspace).
   - App Name: `Momo AI`.
   - Scopes: Request minimal scope: `https://www.googleapis.com/auth/drive.file` (access only to files created or opened by Momo, avoiding broad Drive access).
5. Create Credentials:
   - **Create Credentials** → **OAuth client ID** → Application type: **Android** (or Web application for backend).
6. In Momo AI app:
   - Go to **Files** tab → Tap **Connect** → Authorize your Google account.
   - Momo will create and manage a dedicated folder: `Momo AI / Automations`.

---

## 4. AI Provider Configuration

Momo AI supports:
- **Autonomous Local (Default)**: Runs without requiring any API key or subscription, using Momo's built-in intelligent rule planner.
- **Gemini (Recommended)**: Set provider to **Gemini**, input your Google AI Gemini API Key (e.g. from Google AI Studio Secrets), model: `gemini-1.5-flash` or `gemini-2.5-flash`.
- **OpenAI Compatible**: Compatible with Groq, OpenRouter, Together AI, or local Ollama instances (`http://10.0.2.2:11434/v1`).

---

## 5. Security & Permission Approvals

Momo AI enforces strict owner protections:
- **Approval Gate**: Before sensitive actions (sending messages/emails, online checkouts/payments, deleting files, submitting critical forms), Momo halts execution, sets status to `WAITING_APPROVAL`, and presents an **Approval Dialog**.
- **No Password Storing**: Real passwords and sensitive secrets are never stored in memory or Room DB.
- **Durable Checkpointing**: If the app or device restarts, all tasks and intermediate step outputs are safely preserved in the database.

---

## 6. Testing Workflows

1. **Test Autonomous Task Execution**:
   - In the **Momo** tab, enter: `"Search tech headlines & summarize"`
   - Watch the live activity feed execute `browser_open`, `browser_read_page`, `browser_screenshot`, and `memory_save`.
2. **Test Safety Approval Dialog**:
   - Enter: `"Order item online and proceed to checkout"`
   - Momo pauses at the sensitive checkout click and prompts: *"Momo Needs Your Approval"*.
   - Tap **Approve & Continue** to proceed or **Decline & Stop** to cancel safely.
3. **Test Memory Vault**:
   - Go to **Memory** tab. Search existing memories or tap **+** to add a custom fact or preference.
4. **Test Browser Viewport**:
   - Go to **Browser** tab. Observe address bar, status badge, rendered viewport, and extracted DOM.
5. **Test Google Drive Files**:
   - Go to **Files** tab. View synced screenshots and reports, tap **Connect** to link your account.
