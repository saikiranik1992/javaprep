package com.architect.interviewportal.model;

public record IdeExecutionResponse(
    boolean success,
    String runtimeUsed,
    long executionTimeMs,
    String stdout,
    String stderr
) {}
