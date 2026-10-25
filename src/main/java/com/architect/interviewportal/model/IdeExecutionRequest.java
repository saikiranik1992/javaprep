package com.architect.interviewportal.model;

public record IdeExecutionRequest(
    String javaVersion,
    String springBootVersion,
    String mode,
    String sourceCode,
    String testArgs
) {}
