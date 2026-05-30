package com.errortracker.dto.kafka;

import com.errortracker.model.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorEventMessage {
    private String projectId;
    private String errorType;
    private String message;
    private String stackTrace;
    private String className;
    private String methodName;
    private Integer lineNumber;
    private String environment;
    private Severity severity;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}