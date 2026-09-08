# Feed Creator Agent Demo

This first version is stateless and does not call an LLM. It uses LangGraph to run
validation, copy generation, text review, and result formatting, then exposes the
compiled graph through FastAPI.

## Run

```powershell
conda activate feed-agent
cd D:\web-code\feed-system\agent-service
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload
```

Health check: `GET http://127.0.0.1:8001/health`

Suggestion endpoint: `POST http://127.0.0.1:8001/api/v1/creator/suggest`

## Test

```powershell
conda run -n feed-agent python -m pytest -q
```

The Spring Boot proxy is `POST /creator-assistant/suggest` and requires the same
Bearer token used by the existing authenticated APIs.
