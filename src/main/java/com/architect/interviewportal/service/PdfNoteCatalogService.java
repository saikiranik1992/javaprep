package com.architect.interviewportal.service;

import com.architect.interviewportal.model.PdfNotebookTab;
import com.architect.interviewportal.model.PdfPageItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PdfNoteCatalogService {

    private final ObjectMapper objectMapper;
    private List<PdfNotebookTab> tabs = new ArrayList<>();

    public PdfNoteCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = new ClassPathResource("data/pdf_catalog.json").getInputStream()) {
            this.tabs = objectMapper.readValue(is, new TypeReference<List<PdfNotebookTab>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load PDF notebook catalog from classpath", e);
        }
    }

    public List<PdfNotebookTab> getAllTabs() {
        return Collections.unmodifiableList(tabs);
    }

    public Optional<PdfNotebookTab> getTabById(String tabId) {
        return tabs.stream().filter(t -> t.id().equalsIgnoreCase(tabId)).findFirst();
    }

    public List<PdfPageItem> filterPages(String tabId, String chapter, String query, boolean questionsOnly) {
        return getTabById(tabId)
                .map(tab -> tab.pages().stream()
                        .filter(p -> !questionsOnly || p.isQuestion())
                        .filter(p -> chapter == null || chapter.isBlank() || "ALL".equalsIgnoreCase(chapter) || p.chapter().equalsIgnoreCase(chapter))
                        .filter(p -> {
                            if (query == null || query.isBlank()) return true;
                            String q = query.toLowerCase();
                            return p.title().toLowerCase().contains(q)
                                    || p.fullText().toLowerCase().contains(q)
                                    || p.qId().toLowerCase().contains(q);
                        })
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
}
