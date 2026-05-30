package com.errortracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    private Project project;

    /*
     * Link notification
     * to actual error
     */
    @Column(nullable = false)
    private String errorEventId;

    @Column(nullable = false)
    private String errorType;

    private String className;

    private String methodName;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private Boolean bookmarked = false;

    @Builder.Default
    private Boolean readStatus = false;

    @Builder.Default
    private Boolean deleted = false;

    @Builder.Default
    private LocalDateTime createdAt =
            LocalDateTime.now();

    @PrePersist
    public void init() {

        if (bookmarked == null) {
            bookmarked = false;
        }

        if (readStatus == null) {
            readStatus = false;
        }

        if (deleted == null) {
            deleted = false;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

    }

}