package com.errortracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_patterns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    private String fingerprint;
    private String errorType;
    private String className;
    private String methodName;
    private Integer lineNumber;

    @Builder.Default
    private Integer occurrenceCount = 1;

    @Builder.Default
    private LocalDateTime firstSeen = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();
}