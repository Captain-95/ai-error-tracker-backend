package com.errortracker.controller;

import com.errortracker.dto.response.ApiResponse;
import com.errortracker.dto.response.ErrorEventResponse;
import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.ratelimit.RateLimit;
import com.errortracker.service.ErrorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
public class ErrorController {

    private final ErrorService errorService;

    @GetMapping("/project/{projectId}")
    @RateLimit(
            key = "errors:list",
            limit = 60,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.USER_EMAIL
    )
    public ResponseEntity<ApiResponse<Page<ErrorEventResponse>>> getErrors(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstSeen") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sort = direction.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<ErrorEventResponse> errors = errorService.getProjectErrors(
                projectId, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success("Errors fetched", errors));
    }

    @GetMapping("/{errorId}")
    public ResponseEntity<ApiResponse<ErrorEventResponse>> getError(
            @PathVariable String errorId) {

        return ResponseEntity.ok(
                ApiResponse.success("Error fetched", errorService.getError(errorId)));
    }

    @PatchMapping("/{errorId}/status")
    public ResponseEntity<ApiResponse<ErrorEventResponse>> updateStatus(
            @PathVariable String errorId,
            @RequestParam ErrorStatus status,
            Authentication authentication) {

        String updatedBy = authentication.getName();
        return ResponseEntity.ok(
                ApiResponse.success("Status updated",
                        errorService.updateStatus(errorId, status, updatedBy)));
    }

    @PostMapping("/{errorId}/retry-ai")
    @RateLimit(
            key = "ai:retry",
            limit = 5,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.USER_EMAIL
    )
    public ResponseEntity<ApiResponse<Void>> retryAiAnalysis(
            @PathVariable String errorId) {

        errorService.retryAiAnalysis(errorId);
        return ResponseEntity.ok(ApiResponse.success("AI analysis requeued", null));
    }
}