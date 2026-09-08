import re

from app.state import CreatorState


MAX_TAGS = 5


def validate_input(state: CreatorState) -> CreatorState:
    topic = " ".join(state.get("topic", "").split())
    if not topic:
        raise ValueError("topic must not be blank")
    return {
        "topic": topic,
        "style": state.get("style", "专业简洁").strip() or "专业简洁",
        "target_audience": state.get("target_audience", "普通用户").strip() or "普通用户",
        "warnings": [],
    }


def generate_content(state: CreatorState) -> CreatorState:
    topic = state["topic"]
    audience = state["target_audience"]
    style = state["style"]
    candidates = [
        f"{topic}：快速入门",
        f"10 分钟理解{topic}",
        f"从零开始学习{topic}",
    ]
    original_title = state.get("original_title", "").strip()
    if original_title:
        candidates[-1] = f"{original_title}｜{topic}实战"
    tags = _build_tags(topic)
    hashtags = " ".join(f"#{tag}" for tag in tags)
    return {
        "title_candidates": candidates,
        "recommended_title": candidates[0],
        "description": f"面向{audience}，用{style}的方式介绍{topic}的核心概念和实际应用。\n\n{hashtags}",
        "tags": tags,
    }


def review_content(state: CreatorState) -> CreatorState:
    warnings = list(state.get("warnings", []))
    content = state.get("recommended_title", "") + state.get("description", "")
    contact_pattern = r"(?:微信|加微|vx|qq|电话)\s*[:：]?\s*[a-zA-Z0-9_-]{5,}"
    if re.search(contact_pattern, content, re.IGNORECASE):
        warnings.append("内容疑似包含外部联系方式，请确认后再发布")
    if len(state.get("recommended_title", "")) > 80:
        warnings.append("推荐标题较长，建议缩短到 80 个字符以内")
    return {"risk_level": "MEDIUM" if warnings else "LOW", "warnings": warnings}


def format_result(state: CreatorState) -> CreatorState:
    return {
        "title_candidates": state.get("title_candidates", [])[:3],
        "recommended_title": state.get("recommended_title", ""),
        "description": state.get("description", ""),
        "tags": state.get("tags", [])[:MAX_TAGS],
        "risk_level": state.get("risk_level", "UNKNOWN"),
        "warnings": state.get("warnings", []),
    }


def _build_tags(topic: str) -> list[str]:
    pieces = re.findall(r"[A-Za-z][A-Za-z0-9.+-]*|[\u4e00-\u9fff]{2,8}", topic)
    tags: list[str] = []
    for piece in [topic.replace(" ", ""), *pieces, "短视频"]:
        tag = piece.strip().lstrip("#")
        if tag and tag not in tags:
            tags.append(tag)
        if len(tags) == MAX_TAGS:
            break
    return tags
