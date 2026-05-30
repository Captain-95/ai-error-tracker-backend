package com.errortracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private Long totalErrors;
    private Long newErrors;
    private Long resolvedErrors;
    private Long criticalErrors;
    private Double resolutionRate;
    private Map<String, Long> errorsByStatus;
    private Map<String, Long> errorsBySeverity;
    private List<TrendPoint> errorTrend;
    private List<ErrorEventResponse> topErrors;

    @Data
    @Builder
    public static class TrendPoint {
        private String date;
        private Long count;
    }
}