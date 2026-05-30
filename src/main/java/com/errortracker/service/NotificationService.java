package com.errortracker.service;

import com.errortracker.dto.NotificationDto;
import com.errortracker.model.*;
import com.errortracker.repository.NotificationRepository;
import com.errortracker.repository.ProjectNotificationRecipientRepository;
import com.errortracker.repository.ProjectRepository;
import com.errortracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectNotificationRecipientRepository recipientRepo;

    private User currentUser() {

        String email = SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void create(Project project, ErrorEvent error) {

        Project managedProject = projectRepository
                .findById(project.getId())
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        Notification notification = Notification.builder()
                .project(managedProject)
                .user(managedProject.getUser())
                .errorEventId(error.getId())
                .errorType(error.getErrorType())
                .className(error.getClassName())
                .methodName(error.getMethodName())
                .message(error.getMessage())
                .build();

        repo.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(
            Boolean bookmarked
    ) {

        User user = currentUser();

        List<Notification> notifications =
                bookmarked == null

                        ? repo
                        .findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(
                                user.getId()
                        )

                        : repo
                        .findByUser_IdAndDeletedFalseAndBookmarked(
                                user.getId(),
                                bookmarked
                        );

        return notifications
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void toggleBookmark(
            String id
    ) {

        Notification n =
                repo
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow();

        n.setBookmarked(
                !n.getBookmarked()
        );

        repo.save(n);
    }

    @Transactional
    public void markRead(
            String id
    ) {

        Notification n =
                repo
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow();

        n.setReadStatus(
                true
        );

        repo.save(n);
    }

    @Transactional
    public void delete(
            String id
    ) {

        Notification n =
                repo
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow();

        n.setDeleted(
                true
        );

        repo.save(n);
    }

    @Transactional
    public void deleteAll() {

        User user = currentUser();

        List<Notification> notifications =
                repo
                        .findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(
                                user.getId()
                        );

        notifications.forEach(
                x ->
                        x.setDeleted(
                                true
                        )
        );

        repo.saveAll(
                notifications
        );
    }

    public long count() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return repo.countByUser_IdAndReadStatusFalseAndDeletedFalse(
                user.getId()
        );
    }

    private NotificationDto toDto(
            Notification notification
    ) {

        Project project =
                notification.getProject();

        List<String> recipients =
                recipientRepo
                        .findByProjectId(
                                project.getId()
                        )
                        .stream()
                        .map(
                                ProjectNotificationRecipient::getEmail
                        )
                        .toList();

        return NotificationDto
                .builder()
                .id(notification.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .sentAt(notification.getCreatedAt())
                .recipients(recipients)
                .errorType(notification.getErrorType())
                .className(notification.getClassName())
                .methodName(notification.getMethodName())
                .bookmarked(notification.getBookmarked())
                .read(notification.getReadStatus())
                .build();
    }

}