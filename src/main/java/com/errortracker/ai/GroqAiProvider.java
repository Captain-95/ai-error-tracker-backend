package com.errortracker.ai;

import com.errortracker.model.ErrorEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component("groq")
@RequiredArgsConstructor
@Slf4j
public class GroqAiProvider implements AiProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.groq.api-key}")
    private String apiKey;

    @Value("${app.ai.groq.base-url}")
    private String baseUrl;

    @Value("${app.ai.groq.model}")
    private String model;

    @Value("${app.ai.groq.max-tokens}")
    private int maxTokens;

    @Value("${app.ai.groq.temperature}")
    private double temperature;

    @Override
    public AiAnalysisResult analyze(ErrorEvent errorEvent) {
        try {
            String prompt = buildPrompt(errorEvent);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content",
                                    "You are an expert software engineer specializing in debugging. " +
                                            "Always respond ONLY with valid JSON. " +
                                            "No markdown, no explanation outside the JSON object."
                            ),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "max_tokens", maxTokens,
                    "temperature", temperature
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            return parseGroqResponse(response.getBody());

        } catch (Exception e) {
            log.error("Groq AI analysis failed: {}", e.getMessage());
            return fallbackResult("Groq error: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    private String buildPrompt(ErrorEvent error) {
        // Trim stack trace to avoid exceeding token limit
        String stackTrace = error.getStackTrace() != null
                ? error.getStackTrace().substring(0, Math.min(error.getStackTrace().length(), 2000))
                : "N/A";

        return String.format("""
            Analyze this software error and respond ONLY in this exact JSON format with no extra text:
            {
              "explanation": "clear plain English explanation of what this error means",
              "rootCause": "the actual root cause of why this error happened",
              "solution": "step by step instructions to fix this error",
              "codeExample": "a short code snippet showing the fix, or null if not applicable"
            }

            Error Details:
            - Error Type: %s
            - Message: %s
            - Class: %s
            - Method: %s
            - Line Number: %s
            - Environment: %s

            Stack Trace:
            %s
            """,
                error.getErrorType(),
                error.getMessage(),
                error.getClassName(),
                error.getMethodName(),
                error.getLineNumber(),
                error.getEnvironment(),
                stackTrace
        );
    }

    private AiAnalysisResult parseGroqResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        // Strip accidental markdown fences if model adds them
        content = content
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();

        JsonNode parsed = objectMapper.readTree(content);

        return AiAnalysisResult.builder()
                .explanation(parsed.path("explanation").asText("Unable to parse explanation"))
                .rootCause(parsed.path("rootCause").asText("Unable to parse root cause"))
                .solution(parsed.path("solution").asText("Unable to parse solution"))
                .codeExample(parsed.path("codeExample").isNull()
                        ? null : parsed.path("codeExample").asText())
                .providerName("groq")
                .modelName(model)
                .build();
    }

    private AiAnalysisResult fallbackResult(String reason) {
        return AiAnalysisResult.builder()
                .explanation("AI analysis failed. Please retry.")
                .rootCause("Unable to determine — " + reason)
                .solution("Please review the stack trace manually.")
                .codeExample(null)
                .providerName("groq")
                .modelName(model)
                .build();
    }
}