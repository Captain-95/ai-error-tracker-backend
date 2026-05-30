package com.errortracker.service;

import com.errortracker.dto.kafka.ErrorEventMessage;
import com.errortracker.dto.request.ErrorEventRequest;
import com.errortracker.dto.response.AiSuggestionResponse;
import com.errortracker.dto.response.ErrorEventResponse;
import com.errortracker.exception.ResourceNotFoundException;
import com.errortracker.model.AiSuggestion;
import com.errortracker.model.ErrorEvent;
import com.errortracker.model.Project;
import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.model.enums.Severity;
import com.errortracker.queue.RedisMessageQueue;
import com.errortracker.repository.AiSuggestionRepository;
import com.errortracker.repository.ErrorEventRepository;
import com.errortracker.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorService {

    private final ErrorEventRepository errorEventRepository;
    private final ProjectRepository projectRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
//    private final ErrorEventProducer errorEventProducer;
    private final AiAnalysisService aiAnalysisService;
    private final ProjectService projectService;
    private final RedisMessageQueue redisMessageQueue;

    /**
     * Called by SdkController.
     * Validates the SDK key then pushes the error into Kafka.
     */
    public void receiveFromSdk(String sdkApiKey, ErrorEventRequest request) {
        Project project = projectRepository.findBySdkApiKey(sdkApiKey)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid SDK API key"));

        if (!project.isActive()) {
            throw new ResourceNotFoundException("Project is inactive");
        }

        Severity severity = request.getSeverity() != null
                ? request.getSeverity()
                : calculateSeverity(request.getErrorType());

        ErrorEventMessage message = ErrorEventMessage.builder()
                .projectId(project.getId())
                .errorType(request.getErrorType())
                .message(request.getMessage())
                .stackTrace(request.getStackTrace())
                .className(request.getClassName())
                .methodName(request.getMethodName())
                .lineNumber(request.getLineNumber())
                .environment(request.getEnvironment() != null
                        ? request.getEnvironment() : "unknown")
                .severity(severity)
                .build();

        // Push to Redis queue instead of Kafka
        redisMessageQueue.push(message);
        log.info("Error queued via Redis for project: {}", project.getName());
    }

    /**
     * Called by Kafka consumer after message is received.
     * Saves to DB with deduplication then triggers async AI analysis.
     */
    @Transactional
    public void processIncomingError(ErrorEventMessage message) {
        Project project = projectRepository.findById(message.getProjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found: " + message.getProjectId()));

        String fingerprint = calculateFingerprint(
                message.getProjectId(),
                message.getErrorType(),
                message.getClassName(),
                message.getMethodName(),
                message.getLineNumber()
        );

        // Deduplication — same error just increments the count
        Optional<ErrorEvent> existing = errorEventRepository.findByFingerprintAndProjectId(fingerprint, project.getId());

        if (existing.isPresent()) {
            ErrorEvent event = existing.get();
            event.setOccurrenceCount(event.getOccurrenceCount() + 1);
            event.setLastSeen(LocalDateTime.now());
            errorEventRepository.save(event);
            log.info("Duplicate error — incremented occurrence count for: {}", event.getId());

            try {
                projectService.notifyProjectUsers(project, event);
            }
            catch (Exception ex) {
                log.error(
                        "Notification failed for duplicate error. projectId={}",
                        project.getId(),
                        ex
                );
            }

            return;
        }

        // New unique error — save it
        ErrorEvent event = ErrorEvent.builder()
                .project(project)
                .errorType(message.getErrorType())
                .message(message.getMessage())
                .stackTrace(message.getStackTrace())
                .className(message.getClassName())
                .methodName(message.getMethodName())
                .lineNumber(message.getLineNumber())
                .environment(message.getEnvironment())
                .severity(message.getSeverity())
                .fingerprint(fingerprint)
                .build();

        event = errorEventRepository.save(event);
        log.info("New error saved: id={}, type={}", event.getId(), event.getErrorType());

        /*
         * Send notifications
         * + send mail
         * + create notification rows
         */
        try {
            projectService.notifyProjectUsers(project, event);
        }
        catch (Exception ex) {
            log.error("Notification failed for project {}", project.getId(), ex);
        }

        // Trigger AI analysis asynchronously
        aiAnalysisService.analyzeAsync(event.getId());
    }

    @Transactional(readOnly = true)
    public Page<ErrorEventResponse> getProjectErrors(String projectId, Pageable pageable) {
        return errorEventRepository
                .findByProjectId(projectId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ErrorEventResponse getError(String errorId) {
        ErrorEvent event = errorEventRepository.findDetailedById(errorId)
                .orElseThrow(() -> new ResourceNotFoundException("Error not found: " + errorId));

        return toResponse(event);
    }

    @Transactional
    public ErrorEventResponse updateStatus(String errorId,
                                           ErrorStatus newStatus,
                                           String resolvedBy) {
        ErrorEvent event = errorEventRepository.findById(errorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Error not found: " + errorId));

        event.setStatus(newStatus);

        if (newStatus == ErrorStatus.RESOLVED) {
            event.setResolvedAt(LocalDateTime.now());
            event.setResolvedBy(resolvedBy);
        }

        errorEventRepository.save(event);
        return toResponse(event);
    }

    @Transactional
    public void retryAiAnalysis(String errorId) {

        ErrorEvent event = errorEventRepository.findById(errorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Error not found: " + errorId));

        // reset AI state
        event.setAiAnalyzed(false);

        errorEventRepository.save(event);

        // run AI again
        aiAnalysisService.analyzeAsync(errorId);

        log.info("AI analysis retry queued for error: {}", errorId);
    }

    // ========== HELPERS ==========

    private String calculateFingerprint(String projectId, String errorType,
                                        String className, String methodName,
                                        Integer lineNumber) {
        String raw = projectId + "|" + errorType + "|" +
                className + "|" + methodName + "|" + lineNumber;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("SHA-256 failed, using hashCode fallback");
            return String.valueOf(raw.hashCode());
        }
    }

    private Severity calculateSeverity(String errorType) {
        if (errorType == null) return Severity.MEDIUM;
        String lower = errorType.toLowerCase();

        if (lower.contains("outofmemory") || lower.contains("stackoverflow") ||
                lower.contains("fatal") || lower.contains("oom")) {
            return Severity.CRITICAL;
        }
        if (lower.contains("nullpointer") || lower.contains("classcast") ||
                lower.contains("security") || lower.contains("auth")) {
            return Severity.HIGH;
        }
        if (lower.contains("io") || lower.contains("sql") ||
                lower.contains("timeout") || lower.contains("connection")) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }

    public ErrorEventResponse toResponse(ErrorEvent event) {
        AiSuggestionResponse aiResponse = null;

        if (event.getAiSuggestion() != null) {
            AiSuggestion s = event.getAiSuggestion();
            aiResponse = AiSuggestionResponse.builder()
                    .id(s.getId())
                    .explanation(s.getExplanation())
                    .rootCause(s.getRootCause())
                    .solution(s.getSolution())
                    .codeExample(s.getCodeExample())
                    .aiProvider(s.getAiProvider())
                    .aiModel(s.getAiModel())
                    .createdAt(s.getCreatedAt())
                    .build();
        }

        return ErrorEventResponse.builder()
                .id(event.getId())
                .projectId(event.getProject().getId())
                .projectName(event.getProject().getName())
                .errorType(event.getErrorType())
                .message(event.getMessage())
                .stackTrace(event.getStackTrace())
                .className(event.getClassName())
                .methodName(event.getMethodName())
                .lineNumber(event.getLineNumber())
                .environment(event.getEnvironment())
                .severity(event.getSeverity())
                .status(event.getStatus())
                .occurrenceCount(event.getOccurrenceCount())
                .firstSeen(event.getFirstSeen())
                .lastSeen(event.getLastSeen())
                .resolvedAt(event.getResolvedAt())
                .resolvedBy(event.getResolvedBy())
                .aiAnalyzed(event.isAiAnalyzed())
                .aiSuggestion(aiResponse)
                .build();
    }
}