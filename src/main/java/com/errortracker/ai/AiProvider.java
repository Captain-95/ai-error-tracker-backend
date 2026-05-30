package com.errortracker.ai;

import com.errortracker.model.ErrorEvent;

public interface AiProvider {
    AiAnalysisResult analyze(ErrorEvent errorEvent);
    String getProviderName();
}