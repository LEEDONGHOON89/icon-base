package com.itmasters.icon.api.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI 웹훅 호출 공통 클라이언트
 * - n8n 웹훅 API 호출을 위한 공통 로직 제공
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiWebhookClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

//    @Value("${ai.webhook.api-key}")
    private String apiKey;

    /**
     * 웹훅 API 호출 (POST)
     *
     * @param url 웹훅 URL
     * @param body 요청 바디 (Map 또는 Object)
     * @param responseType 응답 타입
     * @return 응답 객체
     */
    public <T> T post(String url, Object body, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            log.info("Calling AI webhook: {}", url);
            T response = restTemplate.postForObject(url, entity, responseType);
            log.info("AI webhook response received from: {}", url);

            return response;
        } catch (Exception e) {
            log.error("AI webhook call failed: {} - {}", url, e.getMessage(), e);
            throw new RuntimeException("AI 웹훅 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 웹훅 API 호출 후 JSON 응답에서 특정 필드 추출
     *
     * @param url 웹훅 URL
     * @param body 요청 바디
     * @param fieldName 추출할 필드명
     * @return 필드 값 (문자열)
     */
    public String postAndExtractField(String url, Object body, String fieldName) {
        try {
            String responseJson = post(url, body, String.class);

            if (responseJson == null || responseJson.isEmpty()) {
                return null;
            }

            Map<String, Object> responseMap = objectMapper.readValue(responseJson,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            Object fieldValue = responseMap.get(fieldName);
            return fieldValue != null ? fieldValue.toString() : null;
        } catch (Exception e) {
            log.error("Failed to extract field '{}' from response: {}", fieldName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 웹훅 API 호출 (응답 무시)
     *
     * @param url 웹훅 URL
     * @param body 요청 바디
     */
    public void postFireAndForget(String url, Object body) {
        try {
            post(url, body, String.class);
        } catch (Exception e) {
            // 로그만 남기고 예외는 무시
            log.warn("Fire-and-forget webhook call failed (ignored): {}", e.getMessage());
        }
    }
}
