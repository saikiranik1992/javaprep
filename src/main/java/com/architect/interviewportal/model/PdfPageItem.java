package com.architect.interviewportal.model;

public record PdfPageItem(
    int page,
    String qId,
    boolean isQuestion,
    String chapter,
    String title,
    String fullText,
    String shortAnswer,
    String visualFlow,
    String codeExample,
    String explanation,
    String pitfall,
    String followup
) {}
