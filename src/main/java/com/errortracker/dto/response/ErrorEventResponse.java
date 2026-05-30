package com.errortracker.dto.response;

import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.model.enums.Severity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorEventResponse {
    private String id;
    private String projectId;
    private String projectName;
    private String errorType;
    private String message;
    private String stackTrace;
    private String className;
    private String methodName;
    private Integer lineNumber;
    private String environment;
    private Severity severity;
    private ErrorStatus status;
    private Integer occurrenceCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private boolean aiAnalyzed = false;
    private AiSuggestionResponse aiSuggestion;
}