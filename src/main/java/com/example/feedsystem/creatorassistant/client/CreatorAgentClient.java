package com.example.feedsystem.creatorassistant.client;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.creatorassistant.dto.CreatorSuggestRequest;
import com.example.feedsystem.creatorassistant.dto.CreatorSuggestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreatorAgentClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI suggestUri;

    public CreatorAgentClient(
            ObjectMapper objectMapper,
            @Value("${creator-agent.base-url:http://127.0.0.1:8001}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.suggestUri = URI.create(trimRight(baseUrl) + "/api/v1/creator/suggest");
    }

    public CreatorSuggestResponse suggest(CreatorSuggestRequest request) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(toPayload(request));
            HttpRequest httpRequest = HttpRequest.newBuilder(suggestUri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Creator agent rejected request: status={}, response={}",
                        response.statusCode(), response.body());
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI 创作助手请求失败，请查看后端日志中的具体信息");
            }
            return objectMapper.readValue(response.body(), CreatorSuggestResponse.class);
        } catch (BusinessException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize or parse creator agent JSON", ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 创作助手JSON处理失败");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 创作助手请求被中断");
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("Creator agent call failed", ex);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 创作助手暂时不可用，请稍后重试或手动填写发布信息");
        }
    }

    private Map<String, String> toPayload(CreatorSuggestRequest request) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("topic", valueOrEmpty(request.getTopic()));
        payload.put("original_title", valueOrEmpty(request.getOriginalTitle()));
        payload.put("original_description", valueOrEmpty(request.getOriginalDescription()));
        payload.put("style", valueOrDefault(request.getStyle(), "专业简洁"));
        payload.put("target_audience", valueOrDefault(request.getTargetAudience(), "普通用户"));
        return payload;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String trimRight(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }
}
