package com.errortracker.controller;

import com.errortracker.dto.response.ApiResponse;
import com.errortracker.dto.response.DashboardStatsResponse;
import com.errortracker.ratelimit.RateLimit;
import com.errortracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats/{projectId}")
    @RateLimit(
            key = "dashboard:stats",
            limit = 30,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.USER_EMAIL
    )
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @PathVariable String projectId) {

        return ResponseEntity.ok(
                ApiResponse.success("Stats fetched",
                        dashboardService.getStats(projectId)));
    }
}