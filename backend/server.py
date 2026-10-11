"""
Mini Robot Assistant - Secure Gemini AI Backend Proxy
======================================================
This backend holds your GEMINI_API_KEY securely in an environment variable,
preventing API keys from ever being embedded into the Android APK.

Quickstart:
    1. Set your environment variable:
       export GEMINI_API_KEY="AIzaSy..."   (Linux/macOS)
       $env:GEMINI_API_KEY="AIzaSy..."    (Windows PowerShell)

    2. Install dependencies:
       pip install fastapi uvicorn requests

    3. Run server:
       uvicorn server:app --host 0.0.0.0 --port 8000

    4. In MiniRobotAssistant app:
       Open Settings -> Set AI Provider to Gemini -> Enter Backend URL:
       - For Android Emulator: http://10.0.2.2:8000
       - For Physical Phone on same Wi-Fi: http://<YOUR_PC_LOCAL_IP>:8000
"""

import os
import json
import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional

app = FastAPI(title="Mini Robot Assistant Gemini Proxy", version="1.0.0")

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "").strip()
DEFAULT_MODEL = os.environ.get("GEMINI_MODEL", "gemini-1.5-flash")

class ToolDef(BaseModel):
    name: str
    description: str
    is_sensitive: bool = False

class PlanRequest(BaseModel):
    query: str
    tools: List[ToolDef] = []
    model: Optional[str] = None

@app.get("/health")
def health_check():
    has_key = bool(GEMINI_API_KEY)
    return {
        "status": "ok",
        "gemini_api_key_configured": has_key,
        "model": DEFAULT_MODEL
    }

@app.post("/api/plan")
def create_plan(request: PlanRequest):
    api_key = GEMINI_API_KEY
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="GEMINI_API_KEY environment variable is not configured on the backend server."
        )

    model = request.model or DEFAULT_MODEL
    tools_desc = "\n".join([f"- {t.name}: {t.description} (sensitive: {t.is_sensitive})" for t in request.tools])

    system_prompt = f"""You are Mini Robot Assistant, a helpful cute companion on an Android phone.
Decide if the user query requires invoking one or more tools in sequence, or a conversational response.
Available tools:
{tools_desc}

Respond STRICTLY in JSON format with this structure:
{{
   "response": "friendly speech for the robot",
   "plan": [
      {{ "tool": "tool_name", "args": {{ "key": "value" }}, "description": "what this step accomplishes" }}
   ],
   "is_sensitive": true/false
}}
"""

    gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": f"{system_prompt}\n\nUser: {request.query}"}]
            }
        ]
    }

    try:
        resp = requests.post(gemini_url, json=payload, timeout=15)
        if resp.status_code != 200:
            raise HTTPException(status_code=resp.status_code, detail=f"Gemini API returned error: {resp.text}")

        data = resp.json()
        candidates = data.get("candidates", [])
        if not candidates:
            return {"response": "I didn't receive an answer from Gemini.", "plan": [], "is_sensitive": False}

        raw_text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
        cleaned = raw_text.replace("```json", "").replace("```", "").strip()

        parsed = json.loads(cleaned)
        return parsed

    except json.JSONDecodeError:
        return {
            "response": "Understood your query.",
            "plan": [],
            "is_sensitive": False
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=True)
