package com.architect.interviewportal.controller;

import com.architect.interviewportal.model.IdeExecutionRequest;
import com.architect.interviewportal.model.IdeExecutionResponse;
import com.architect.interviewportal.model.PdfNotebookTab;
import com.architect.interviewportal.model.PdfPageItem;
import com.architect.interviewportal.service.IdeExecutionService;
import com.architect.interviewportal.service.PdfNoteCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class InterviewPortalController {

    private final PdfNoteCatalogService catalogService;
    private final IdeExecutionService ideExecutionService;

    public InterviewPortalController(PdfNoteCatalogService catalogService, IdeExecutionService ideExecutionService) {
        this.catalogService = catalogService;
        this.ideExecutionService = ideExecutionService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(name = "tab", defaultValue = "core-java") String activeTabId,
            @RequestParam(name = "chapter", defaultValue = "ALL") String selectedChapter,
            @RequestParam(name = "q", defaultValue = "") String searchQuery,
            @RequestParam(name = "questionsOnly", defaultValue = "false") boolean questionsOnly,
            Model model
    ) {
        List<PdfNotebookTab> allTabs = catalogService.getAllTabs();
        PdfNotebookTab activeTab = catalogService.getTabById(activeTabId)
                .orElse(allTabs.getFirst());

        List<PdfPageItem> filteredPages = catalogService.filterPages(
                activeTab.id(), selectedChapter, searchQuery, questionsOnly);

        int totalPagesAllPdfs = allTabs.stream().mapToInt(PdfNotebookTab::pageCount).sum();
        int totalQuestionsAllPdfs = allTabs.stream().mapToInt(PdfNotebookTab::questionCount).sum();

        model.addAttribute("tabs", allTabs);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("activeTabId", activeTab.id());
        model.addAttribute("selectedChapter", selectedChapter);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("questionsOnly", questionsOnly);
        model.addAttribute("filteredPages", filteredPages);
        model.addAttribute("totalPagesAllPdfs", totalPagesAllPdfs);
        model.addAttribute("totalQuestionsAllPdfs", totalQuestionsAllPdfs);
        model.addAttribute("javaVersions", List.of("25 LTS (Latest)", "24", "21 LTS", "17 LTS", "11 LTS", "8 LTS"));
        model.addAttribute("springBootVersions", List.of("3.5.0 (Latest Stable)", "4.0.0-M1 (Next Gen)", "3.4.5", "3.3.10"));

        return "index";
    }

    @GetMapping("/tab/{tabId}/fragment")
    public String getTabContentFragment(
            @PathVariable String tabId,
            @RequestParam(name = "chapter", defaultValue = "ALL") String chapter,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "questionsOnly", defaultValue = "false") boolean questionsOnly,
            Model model
    ) {
        PdfNotebookTab activeTab = catalogService.getTabById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown PDF tab: " + tabId));
        List<PdfPageItem> filteredPages = catalogService.filterPages(tabId, chapter, query, questionsOnly);

        model.addAttribute("activeTab", activeTab);
        model.addAttribute("selectedChapter", chapter);
        model.addAttribute("searchQuery", query);
        model.addAttribute("questionsOnly", questionsOnly);
        model.addAttribute("filteredPages", filteredPages);
        return "fragments/pdf-tab-view :: pdfTabContent";
    }

    @GetMapping("/api/tabs")
    @ResponseBody
    public ResponseEntity<List<PdfNotebookTab>> apiGetAllTabs() {
        return ResponseEntity.ok(catalogService.getAllTabs());
    }

    @GetMapping("/api/tabs/{tabId}/pages")
    @ResponseBody
    public ResponseEntity<List<PdfPageItem>> apiGetTabPages(
            @PathVariable String tabId,
            @RequestParam(name = "chapter", defaultValue = "ALL") String chapter,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "questionsOnly", defaultValue = "false") boolean questionsOnly
    ) {
        return ResponseEntity.ok(catalogService.filterPages(tabId, chapter, query, questionsOnly));
    }

    @PostMapping("/api/ide/execute")
    @ResponseBody
    public ResponseEntity<IdeExecutionResponse> executeIdeCode(@RequestBody IdeExecutionRequest request) {
        return ResponseEntity.ok(ideExecutionService.executeCode(request));
    }

    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "templateEngine", "Spring Boot Thymeleaf SSR",
                "pdfCount", catalogService.getAllTabs().size(),
                "javaRuntime", System.getProperty("java.version")
        );
    }
}
