package com.errortracker.repository;

import com.errortracker.model.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, String> {
    Optional<AiSuggestion> findByErrorEventId(String errorEventId);
}