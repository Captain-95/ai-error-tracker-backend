package com.errortracker.service;

import com.errortracker.dto.NotificationRecipientRequest;
import com.errortracker.dto.request.CreateProjectRequest;
import com.errortracker.dto.response.ProjectResponse;
import com.errortracker.exception.ResourceNotFoundException;
import com.errortracker.model.*;
import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ErrorEventRepository errorEventRepository;
    private final ProjectNotificationRecipientRepository projectNotificationRepo;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final ProjectNotificationRecipientRepository recipientRepo;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User user = getCurrentUser();

        Project project = Project.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .sdkApiKey(UUID.randomUUID().toString())
                .build();

        project = projectRepository.save(project);
        return toResponse(project);
    }

    public List<ProjectResponse> getUserProjects() {
        User user = getCurrentUser();
        return projectRepository
                .findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProject(String projectId) {
        Project project = getProjectAndValidateOwnership(projectId);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse regenerateSdkKey(String projectId) {
        Project project = getProjectAndValidateOwnership(projectId);
        project.setSdkApiKey(UUID.randomUUID().toString());
        projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public void deleteProject(String projectId) {
        Project project = getProjectAndValidateOwnership(projectId);
        project.setActive(false);
        projectRepository.save(project);
    }

    // ========== HELPERS ==========

    private Project getProjectAndValidateOwnership(String projectId) {
        User user = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project not found: " + projectId));

        // Make sure this project belongs to the logged in user
        if (!project.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }

        return project;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Authenticated user not found"));
    }

    public ProjectResponse toResponse(Project project) {
        Long total = errorEventRepository.countByProjectId(project.getId());
        Long unresolved = errorEventRepository
                .countByProjectIdAndStatus(project.getId(), ErrorStatus.NEW);

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .sdkApiKey(project.getSdkApiKey())
                .active(project.isActive())
                .createdAt(project.getCreatedAt())
                .totalErrors(total)
                .unresolvedErrors(unresolved)
                .build();
    }

    @Transactional
    public List<String> saveNotificationRecipients(String projectId, NotificationRecipientRequest request) {

        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));

        projectNotificationRepo.deleteByProjectId(projectId);

        List<ProjectNotificationRecipient> rows = request.getEmails()
                        .stream().map(email -> ProjectNotificationRecipient.builder().project(project).email(email).build()
                ).toList();

        projectNotificationRepo.saveAll(rows);

        return request.getEmails();
    }

    public List<String> getNotificationRecipients(String projectId) {

        return projectNotificationRepo.findByProjectId(projectId).stream()
                .map(ProjectNotificationRecipient::getEmail).toList();
    }

    @Transactional
    public void notifyProjectUsers(Project project, ErrorEvent event) {

        List<String> recipients = recipientRepo.findByProjectId(project.getId())
                        .stream()
                        .map(ProjectNotificationRecipient::getEmail)
                        .toList();

        if (recipients.isEmpty()) { return; }

        for (String email : recipients) {

            mailService.sendErrorMail(project, event, email);

        }

        Notification notification = Notification.builder().project(project).user(project.getUser())
                        .errorEventId(event.getId())
                        .errorType(event.getErrorType())
                        .className(event.getClassName())
                        .methodName(event.getMethodName())
                        .message(event.getMessage())
                        .build();

        notificationService.create(project, event);

    }

}