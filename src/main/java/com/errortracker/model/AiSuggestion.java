package com.errortracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_suggestions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_event_id", nullable = false)
    @JsonIgnore
    private ErrorEvent errorEvent;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(columnDefinition = "TEXT")
    private String codeExample;

    private String aiProvider;
    private String aiModel;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}