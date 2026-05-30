package com.errortracker.controller;

import com.errortracker.dto.NotificationRecipientRequest;
import com.errortracker.dto.request.CreateProjectRequest;
import com.errortracker.dto.response.ApiResponse;
import com.errortracker.dto.response.ProjectResponse;
import com.errortracker.ratelimit.RateLimit;
import com.errortracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @RateLimit(
            key = "project:create",
            limit = 10,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.USER_EMAIL
    )
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Project created", projectService.createProject(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects() {
        return ResponseEntity.ok(
                ApiResponse.success("Projects fetched", projectService.getUserProjects()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable String projectId) {

        return ResponseEntity.ok(
                ApiResponse.success("Project fetched", projectService.getProject(projectId)));
    }

    @PostMapping("/{projectId}/regenerate-key")
    public ResponseEntity<ApiResponse<ProjectResponse>> regenerateKey(
            @PathVariable String projectId) {

        return ResponseEntity.ok(
                ApiResponse.success("SDK key regenerated",
                        projectService.regenerateSdkKey(projectId)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable String projectId) {

        projectService.deleteProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }

    @PutMapping("/{projectId}/notification-recipients")
    public ResponseEntity<?> saveRecipients(@PathVariable String projectId, @RequestBody NotificationRecipientRequest request) {
        return ResponseEntity.ok(projectService.saveNotificationRecipients(projectId, request));
    }


    @GetMapping("/{projectId}/notification-recipients")
    public ResponseEntity<?> getRecipients(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getNotificationRecipients(projectId));
    }
}