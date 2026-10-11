# Mini Robot Assistant - AI Backend Setup Guide

This guide explains how to configure and run the secure backend proxy for Google Gemini AI integration.

## Why a Backend Proxy?
Per Android security best practices, sensitive API keys should never be embedded directly into client APKs. By running this backend proxy, your `GEMINI_API_KEY` remains safely stored on your server in an environment variable.

---

## 1. Get a Free Gemini API Key
1. Go to **[Google AI Studio](https://aistudio.google.com/app/apikey)**.
2. Sign in with any Google account.
3. Click **Create API key**.
4. Copy your key. (The free tier provides 15 requests/minute with no credit card required).

---

## 2. Running the Backend Server

### Prerequisites:
- Python 3.9+
- pip

### Step A: Set the Environment Variable
**On Linux / macOS:**
```bash
export GEMINI_API_KEY="AIzaSyYourSecretKeyHere..."
```

**On Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY="AIzaSyYourSecretKeyHere..."
```

**On Windows (Command Prompt):**
```cmd
set GEMINI_API_KEY=AIzaSyYourSecretKeyHere...
```

### Step B: Install Dependencies
```bash
pip install fastapi uvicorn requests
```

### Step C: Start the Server
```bash
python server.py
```
Or with uvicorn:
```bash
uvicorn server:app --host 0.0.0.0 --port 8000
```

The server will be live at `http://0.0.0.0:8000`. You can test it in your browser at `http://localhost:8000/health`.

---

## 3. Connect the Android App

1. Open **Mini Robot Assistant** on your phone.
2. Under **AI Intelligence Engine**, select **Google Gemini LLM**.
3. In **Backend Proxy URL**, enter:
   - If using Android Studio Emulator: `http://10.0.2.2:8000`
   - If using a physical phone on the same Wi-Fi network: `http://<YOUR_PC_IP>:8000` (e.g. `http://192.168.1.50:8000`)
4. Tap **Save AI Configuration**.

*(Note: For rapid development without running a server, you can also paste your Gemini API key directly into the Gemini API Key field in the app. The key is stored locally in private on-device SharedPreferences and is never logged).*
