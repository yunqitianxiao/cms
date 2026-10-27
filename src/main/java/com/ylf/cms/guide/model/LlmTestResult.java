package com.ylf.cms.guide.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmTestResult {

    private boolean ok;
    private long latencyMs;
    private String message;
}
