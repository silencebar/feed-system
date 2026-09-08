from app.graph import graph


def test_creator_graph_returns_publishable_copy() -> None:
    result = graph.invoke({
        "topic": "Spring Boot 微服务",
        "style": "专业简洁",
        "target_audience": "Java 初学者",
    })
    assert len(result["title_candidates"]) == 3
    assert result["recommended_title"] == result["title_candidates"][0]
    assert "#" in result["description"]
    assert result["tags"]
    assert result["risk_level"] == "LOW"

