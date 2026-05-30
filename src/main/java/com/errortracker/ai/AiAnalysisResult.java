package com.errortracker.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAnalysisResult {
    private String explanation;
    private String rootCause;
    private String solution;
    private String codeExample;
    private String providerName;
    private String modelName;
}