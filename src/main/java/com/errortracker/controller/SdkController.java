package com.errortracker.controller;

import com.errortracker.dto.request.ErrorEventRequest;
import com.errortracker.dto.response.ApiResponse;
import com.errortracker.ratelimit.RateLimit;
import com.errortracker.service.ErrorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
public class SdkController {

    private final ErrorService errorService;

    /**
     * This is the only endpoint the SDK calls.
     * No JWT needed — authenticated via X-API-KEY header.
     *
     * How to call from any project:
     * POST /api/sdk/errors
     * Header: X-API-KEY: <your-project-sdk-key>
     * Body:
     * {
     *   "errorType": "NullPointerException",
     *   "message": "Cannot invoke method on null object",
     *   "stackTrace": "...",
     *   "className": "com.myapp.UserService",
     *   "methodName": "getUserById",
     *   "lineNumber": 42,
     *   "environment": "prod"
     * }
     */
    @PostMapping("/errors")
    @RateLimit(
            key = "sdk:ingest",
            limit = 60,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.API_KEY
    )
    public ResponseEntity<ApiResponse<Void>> receiveError(
            @RequestHeader("X-API-KEY") String sdkApiKey,
            @Valid @RequestBody ErrorEventRequest request) {

        errorService.receiveFromSdk(sdkApiKey, request);
        return ResponseEntity
                .accepted()
                .body(ApiResponse.success("Error received and queued for processing", null));
    }
}