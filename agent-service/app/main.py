from fastapi import FastAPI, HTTPException

from app.graph import graph
from app.schemas import CreatorSuggestRequest, CreatorSuggestResponse


app = FastAPI(title="Feed Creator Agent", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/v1/creator/suggest", response_model=CreatorSuggestResponse)
def suggest(request: CreatorSuggestRequest) -> CreatorSuggestResponse:
    try:
        result = graph.invoke(request.model_dump())
        return CreatorSuggestResponse.model_validate(result)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
