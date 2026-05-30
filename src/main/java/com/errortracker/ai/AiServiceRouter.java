package com.errortracker.ai;

import com.errortracker.model.ErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceRouter {

    // Spring automatically injects ALL AiProvider beans into this map
    // Key = bean name ("groq" or "ollama"), Value = the provider instance
    private final Map<String, AiProvider> providers;

    @Value("${app.ai.provider}")
    private String activeProvider;

    @Value("${app.ai.enabled}")
    private boolean aiEnabled;

    public AiAnalysisResult analyze(ErrorEvent errorEvent) {
        if (!aiEnabled) {
            log.info("AI is disabled via config. Skipping analysis for error: {}", errorEvent.getId());
            return AiAnalysisResult.builder()
                    .explanation("AI analysis is currently disabled.")
                    .rootCause("N/A — AI is turned off in config.")
                    .solution("Set app.ai.enabled=true in your properties file to enable AI.")
                    .providerName("none")
                    .modelName("none")
                    .build();
        }

        AiProvider provider = providers.get(activeProvider);

        if (provider == null) {
            log.error("Unknown AI provider configured: '{}'. Available: {}",
                    activeProvider, providers.keySet());
            // Fallback to first available provider
            provider = providers.values().iterator().next();
            log.warn("Falling back to provider: {}", provider.getProviderName());
        }

        log.info("Routing AI analysis to provider: {} for error: {}",
                provider.getProviderName(), errorEvent.getId());

        return provider.analyze(errorEvent);
    }
}