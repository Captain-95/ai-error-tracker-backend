package com.errortracker.dto.request;

import com.errortracker.model.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ErrorEventRequest {

    @NotBlank(message = "errorType is required")
    private String errorType;

    private String message;
    private String stackTrace;
    private String className;
    private String methodName;
    private Integer lineNumber;
    private String environment;
    private Severity severity;
}