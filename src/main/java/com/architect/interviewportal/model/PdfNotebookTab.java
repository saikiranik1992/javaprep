package com.architect.interviewportal.model;

import java.util.List;

public record PdfNotebookTab(
    String id,
    String filename,
    String tabTitle,
    String fullTitle,
    String badge,
    String icon,
    String color,
    String versionTag,
    String description,
    int pageCount,
    int questionCount,
    List<String> chapters,
    List<PdfPageItem> pages
) {}
