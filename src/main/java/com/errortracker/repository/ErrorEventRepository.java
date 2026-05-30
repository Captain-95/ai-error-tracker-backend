package com.errortracker.repository;

import com.errortracker.model.ErrorEvent;
import com.errortracker.model.enums.ErrorStatus;
import com.errortracker.model.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ErrorEventRepository extends JpaRepository<ErrorEvent, String> {

    // Deduplication check
    Optional<ErrorEvent> findByFingerprintAndProjectId(String fingerprint, String projectId);

    // Paginated list for dashboard
    Page<ErrorEvent> findByProjectId(String projectId, Pageable pageable);
    Page<ErrorEvent> findByProjectIdAndStatus(String projectId, ErrorStatus status, Pageable pageable);
    Page<ErrorEvent> findByProjectIdAndSeverity(String projectId, Severity severity, Pageable pageable);

    // Count queries for stats cards
    Long countByProjectId(String projectId);
    Long countByProjectIdAndStatus(String projectId, ErrorStatus status);
    Long countByProjectIdAndSeverity(String projectId, Severity severity);

    // Top 5 most recurring errors
    List<ErrorEvent> findTop5ByProjectIdOrderByOccurrenceCountDesc(String projectId);

    // Errors not yet analyzed by AI
    List<ErrorEvent> findByAiAnalyzedFalseAndProjectId(String projectId);

    // Error trend — count per day for last N days (used for line chart)
    @Query("SELECT CAST(e.firstSeen AS date) as date, COUNT(e) as count " +
            "FROM ErrorEvent e " +
            "WHERE e.project.id = :projectId AND e.firstSeen >= :since " +
            "GROUP BY CAST(e.firstSeen AS date) " +
            "ORDER BY CAST(e.firstSeen AS date) ASC")
    List<Object[]> getErrorTrend(@Param("projectId") String projectId,
                                 @Param("since") LocalDateTime since);

    @Query(
            "SELECT e " +
                    "FROM ErrorEvent e " +
                    "LEFT JOIN FETCH e.project " +
                    "LEFT JOIN FETCH e.aiSuggestion " +
                    "WHERE e.id = :id"
    )
    Optional<ErrorEvent> findDetailedById(@Param("id") String id);
}