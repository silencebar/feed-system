from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_suggest() -> None:
    response = client.post(
        "/api/v1/creator/suggest",
        json={"topic": "Spring Boot 微服务", "target_audience": "Java 初学者"},
    )
    assert response.status_code == 200
    assert response.json()["risk_level"] == "LOW"
