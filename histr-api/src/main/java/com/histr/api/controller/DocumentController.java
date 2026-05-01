package com.histr.api.controller;

import com.histr.api.dto.*;
import com.histr.api.service.DocumentService;
import com.histr.api.service.WorkerStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin
public class DocumentController {

    private final DocumentService documentService;
    private final WorkerStatusService workerStatusService;

    @GetMapping("/parse")
    public ResponseEntity<?> parseDocuments() {
        try {
            Map<String, Object> result = documentService.parseLocalDocuments();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = documentService.parseUploadedFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<PagedTransactionsResponse> getTransactions(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return ResponseEntity.ok(documentService.getTransactions(limit, offset, search, startDate, endDate));
    }

    @GetMapping("/transactions/stats")
    public ResponseEntity<StatsResponse> getStats(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return ResponseEntity.ok(documentService.getStats(search, startDate, endDate));
    }

    @GetMapping("/categories/summary")
    public ResponseEntity<CategorySummaryResponse> getCategorySummary(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return ResponseEntity.ok(documentService.getCategorySummary(search, startDate, endDate));
    }

    @GetMapping("/workers/status")
    public ResponseEntity<WorkerStatusResponse> getWorkerStatus() {
        return ResponseEntity.ok(workerStatusService.getStatus());
    }
}
