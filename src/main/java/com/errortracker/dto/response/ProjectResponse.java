package com.errortracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private String id;
    private String name;
    private String description;
    private String sdkApiKey;
    private boolean active;
    private LocalDateTime createdAt;
    private Long totalErrors;
    private Long unresolvedErrors;
}