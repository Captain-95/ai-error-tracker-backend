package com.errortracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NotificationDto {

    private String id;

    private String projectId;

    private String projectName;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime sentAt;

    private List<String> recipients;

    private String errorType;

    private String className;

    private String methodName;

    private Boolean bookmarked;

    private Boolean read;
}
