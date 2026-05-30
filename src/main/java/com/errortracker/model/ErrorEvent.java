package com.errortracker.model;

import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.model.enums.Severity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_events", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_fingerprint", columnList = "fingerprint"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "firstSeen")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @Column(nullable = false)
    private String errorType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    private String className;
    private String methodName;
    private Integer lineNumber;

    private String environment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ErrorStatus status = ErrorStatus.NEW;

    // SHA-256 hash of (projectId + errorType + className + methodName + lineNumber)
    // Same error occurring again just increments occurrenceCount instead of creating new row
    @Column(nullable = false)
    private String fingerprint;

    @Builder.Default
    private Integer occurrenceCount = 1;

    @Builder.Default
    private LocalDateTime firstSeen = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();

    private LocalDateTime resolvedAt;
    private String resolvedBy;

    @Builder.Default
    private boolean aiAnalyzed = false;

    @OneToOne(
            mappedBy = "errorEvent",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @Setter(AccessLevel.NONE)
    private AiSuggestion aiSuggestion;

    public void setAiSuggestion(AiSuggestion aiSuggestion) {
        this.aiSuggestion = aiSuggestion;

        if (aiSuggestion != null) {
            aiSuggestion.setErrorEvent(this);
        }
    }

}