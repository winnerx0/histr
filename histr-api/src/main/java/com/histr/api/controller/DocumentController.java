package com.histr.api.controller;

import com.histr.api.dto.*;
import com.histr.api.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping(value = "/parse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> parseDocuments() {
        try {
            Map<String, Object> result = documentService.parseLocalDocuments();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = documentService.parseUploadedFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedTransactionsResponse> getTransactions(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return ResponseEntity.ok(documentService.getTransactions(pageNo, limit, search, startDate, endDate));
    }

    @GetMapping(value = "/transactions/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StatsResponse> getStats(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return ResponseEntity.ok(documentService.getStats(search, startDate, endDate));
    }

    @GetMapping(value = "/categories/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategorySummaryResponse> getCategorySummary(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return ResponseEntity.ok(documentService.getCategorySummary(search, startDate, endDate));
    }

}
