package com.errortracker.service;

import com.errortracker.dto.response.DashboardStatsResponse;
import com.errortracker.dto.response.ErrorEventResponse;
import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.model.enums.Severity;
import com.errortracker.repository.ErrorEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ErrorEventRepository errorEventRepository;
    private final ErrorService errorService;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(String projectId) {

        // Stat cards
        long total    = errorEventRepository.countByProjectId(projectId);
        long newCount = errorEventRepository.countByProjectIdAndStatus(projectId, ErrorStatus.NEW);
        long resolved = errorEventRepository.countByProjectIdAndStatus(projectId, ErrorStatus.RESOLVED);
        long critical = errorEventRepository.countByProjectIdAndSeverity(projectId, Severity.CRITICAL);

        double resolutionRate = total > 0
                ? Math.round((double) resolved / total * 1000.0) / 10.0
                : 0.0;

        // Errors breakdown by status (for pie/donut chart)
        Map<String, Long> byStatus = Arrays.stream(ErrorStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        status -> errorEventRepository
                                .countByProjectIdAndStatus(projectId, status)
                ));

        // Errors breakdown by severity (for bar chart)
        Map<String, Long> bySeverity = Arrays.stream(Severity.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        severity -> errorEventRepository
                                .countByProjectIdAndSeverity(projectId, severity)
                ));

        // Error trend — last 7 days (for line chart)
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> trendRaw = errorEventRepository.getErrorTrend(projectId, since);

        List<DashboardStatsResponse.TrendPoint> trend = trendRaw.stream()
                .map(row -> DashboardStatsResponse.TrendPoint.builder()
                        .date(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        // Top 5 most recurring errors
        List<ErrorEventResponse> topErrors = errorEventRepository
                .findTop5ByProjectIdOrderByOccurrenceCountDesc(projectId)
                .stream()
                .map(errorService::toResponse)
                .toList();

        return DashboardStatsResponse.builder()
                .totalErrors(total)
                .newErrors(newCount)
                .resolvedErrors(resolved)
                .criticalErrors(critical)
                .resolutionRate(resolutionRate)
                .errorsByStatus(byStatus)
                .errorsBySeverity(bySeverity)
                .errorTrend(trend)
                .topErrors(topErrors)
                .build();
    }
}