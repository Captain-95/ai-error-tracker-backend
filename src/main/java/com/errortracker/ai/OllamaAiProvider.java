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

import java.util.Map;

@Component("ollama")
@RequiredArgsConstructor
@Slf4j
public class OllamaAiProvider implements AiProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${app.ai.ollama.model}")
    private String model;

    @Override
    public AiAnalysisResult analyze(ErrorEvent errorEvent) {
        try {
            String prompt = buildPrompt(errorEvent);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "format", "json"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/generate",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            return parseOllamaResponse(response.getBody());

        } catch (Exception e) {
            log.error("Ollama AI analysis failed: {}", e.getMessage());
            return fallbackResult("Ollama error: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    private String buildPrompt(ErrorEvent error) {

        String stackTrace = error.getStackTrace() != null
                ? error.getStackTrace().substring(0,
                Math.min(error.getStackTrace().length(), 3000))
                : "N/A";

        return String.format("""
        You are a senior Java + Spring Boot engineer.

        Analyze the following production error carefully.

        Respond ONLY with valid JSON.

        JSON format:
        {
          "explanation": "simple explanation of the error",
          "rootCause": "actual root cause",
          "solution": "step by step fix explanation",
          "codeExample": "FULL practical Java/Spring code fix example"
        }

        IMPORTANT:
        - Always provide a real code example.
        - Code example must be practical and production-like.
        - Use Java/Spring Boot best practices.
        - Do NOT return markdown.
        - Do NOT wrap JSON in backticks.
        - Return ONLY raw JSON.

        Error Type: %s
        Message: %s
        Class: %s
        Method: %s
        Line: %s
        Environment: %s

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

    private AiAnalysisResult parseOllamaResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("response").asText();

        content = content
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();

        JsonNode parsed = objectMapper.readTree(content);

        return AiAnalysisResult.builder()
                .explanation(parsed.path("explanation").asText("Unable to parse explanation"))
                .rootCause(parsed.path("rootCause").asText("Unable to parse root cause"))
                .solution(parsed.path("solution").asText("Unable to parse solution"))
                .codeExample(parsed.path("codeExample").asText("").trim())
                .providerName("ollama")
                .modelName(model)
                .build();
    }

    private AiAnalysisResult fallbackResult(String reason) {
        return AiAnalysisResult.builder()
                .explanation("Ollama analysis failed.")
                .rootCause("Error: " + reason)
                .solution("Make sure Ollama is running locally: `ollama serve` then `ollama pull llama3`")
                .codeExample(null)
                .providerName("ollama")
                .modelName(model)
                .build();
    }
}