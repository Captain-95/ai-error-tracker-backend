package com.errortracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiSuggestionResponse {
    private String id;
    private String explanation;
    private String rootCause;
    private String solution;
    private String codeExample;
    private String aiProvider;
    private String aiModel;
    private LocalDateTime createdAt;
}