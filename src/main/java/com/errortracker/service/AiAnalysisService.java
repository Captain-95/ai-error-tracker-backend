package com.errortracker.service;

import com.errortracker.ai.AiAnalysisResult;
import com.errortracker.ai.AiServiceRouter;
import com.errortracker.exception.ResourceNotFoundException;
import com.errortracker.model.AiSuggestion;
import com.errortracker.model.ErrorEvent;
import com.errortracker.repository.AiSuggestionRepository;
import com.errortracker.repository.ErrorEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final ErrorEventRepository errorEventRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final AiServiceRouter aiServiceRouter;

    /**
     * @Async means this runs in a separate thread from aiAnalysisExecutor pool.
     * The Kafka consumer does NOT wait for this to finish.
     * AI calls can take 2-5 seconds — this keeps the consumer fast.
     */
    @Async("aiAnalysisExecutor")
    @Transactional
    public void analyzeAsync(String errorId) {

        try {

            ErrorEvent event = errorEventRepository.findById(errorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Error not found: " + errorId));

            // Skip if already analyzed
            if (event.isAiAnalyzed()) {
                log.info("Error {} already analyzed — skipping", errorId);
                return;
            }

            AiAnalysisResult result = aiServiceRouter.analyze(event);
            AiSuggestion suggestion = event.getAiSuggestion();

            // create if missing
            if (suggestion == null) {
                suggestion = new AiSuggestion();
            }

            suggestion.setExplanation(result.getExplanation());
            suggestion.setRootCause(result.getRootCause());
            suggestion.setSolution(result.getSolution());
            suggestion.setCodeExample(result.getCodeExample());
            suggestion.setAiProvider(result.getProviderName());
            suggestion.setAiModel(result.getModelName());

            // maintain relation
            event.setAiSuggestion(suggestion);

            // SUCCESS
            event.setAiAnalyzed(true);

            errorEventRepository.save(event);

            log.info("AI analysis completed for error: {}", errorId);

        } catch (Exception e) {
            log.error("AI analysis failed for error: {}", errorId, e);
        }
    }
}