from typing import TypedDict


class CreatorState(TypedDict, total=False):
    topic: str
    original_title: str
    original_description: str
    style: str
    target_audience: str
    title_candidates: list[str]
    recommended_title: str
    description: str
    tags: list[str]
    risk_level: str
    warnings: list[str]

