from pydantic import BaseModel, Field


class CreatorSuggestRequest(BaseModel):
    topic: str = Field(min_length=1, max_length=100)
    original_title: str = Field(default="", max_length=120)
    original_description: str = Field(default="", max_length=1000)
    style: str = Field(default="专业简洁", max_length=30)
    target_audience: str = Field(default="普通用户", max_length=50)


class CreatorSuggestResponse(BaseModel):
    title_candidates: list[str]
    recommended_title: str
    description: str
    tags: list[str]
    risk_level: str
    warnings: list[str]
