package com.errortracker.repository;

import com.errortracker.model.ErrorPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErrorPatternRepository extends JpaRepository<ErrorPattern, String> {
    Optional<ErrorPattern> findByFingerprintAndProjectId(String fingerprint, String projectId);
}