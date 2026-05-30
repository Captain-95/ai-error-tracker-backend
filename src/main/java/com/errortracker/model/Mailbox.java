package com.errortracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mailbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mailbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    private Project project;

    @Column(nullable = false)
    private String fromEmail;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @PrePersist
    public void init(){
        if(sentAt==null){
            sentAt = LocalDateTime.now();
        }
    }

}