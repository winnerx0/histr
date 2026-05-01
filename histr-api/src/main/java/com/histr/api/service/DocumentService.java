package com.histr.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histr.api.dto.*;
import com.histr.api.model.Document;
import com.histr.api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String TRANSACTIONS_QUEUE = "transactions";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.max-upload-size-mb:15}")
    private long maxUploadSizeMb;

    public Map<String, Object> parseLocalDocuments() throws IOException {
        Path docsPath = Path.of(System.getProperty("user.dir"), "documents");
        Files.createDirectories(docsPath);

        File[] files = docsPath.toFile().listFiles(f ->
                f.getName().matches("(?i).*\\.(xlsx|csv)"));

        int queuedBatches = 0;
        if (files != null) {
            for (File file : files) {
                List<List<String>> data = parseFile(file.toPath(), file.getName());
                if (!data.isEmpty()) {
                    pushToQueue(data);
                    queuedBatches++;
                }
            }
        }

        return Map.of("message", "Documents parsed successfully", "queuedBatches", queuedBatches);
    }

    public Map<String, Object> parseUploadedFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!filename.matches("(?i).*\\.(xlsx|csv)")) {
            throw new IllegalArgumentException("Only .xlsx and .csv files are supported");
        }
        if (file.getSize() > maxUploadSizeMb * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds " + maxUploadSizeMb + "MB size limit");
        }

        List<List<String>> data = parseMultipartFile(file, filename);
        int queuedBatches = 0;
        if (!data.isEmpty()) {
            pushToQueue(data);
            queuedBatches = 1;
        }

        return Map.of(
                "message", "File parsed and queued for processing",
                "fileName", filename,
                "queuedBatches", queuedBatches
        );
    }

    private List<List<String>> parseMultipartFile(MultipartFile file, String filename) throws IOException {
        if (filename.matches("(?i).*\\.csv")) {
            return parseCsvStream(file.getInputStream());
        }
        return parseXlsxStream(file.getInputStream());
    }

    private List<List<String>> parseFile(Path path, String filename) throws IOException {
        if (filename.matches("(?i).*\\.csv")) {
            return parseCsvStream(Files.newInputStream(path));
        }
        return parseXlsxStream(Files.newInputStream(path));
    }

    private List<List<String>> parseXlsxStream(InputStream is) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return rows;
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                int lastCell = row.getLastCellNum();
                for (int i = 0; i < lastCell; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cells.add(formatter.formatCellValue(cell));
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private List<List<String>> parseCsvStream(InputStream is) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                record.forEach(row::add);
                rows.add(row);
            }
        }
        return rows;
    }

    private void pushToQueue(List<List<String>> data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        redis.opsForList().leftPush(TRANSACTIONS_QUEUE, json);
    }

    public PagedTransactionsResponse getTransactions(
            int limit, int offset, String search,
            OffsetDateTime startDate, OffsetDateTime endDate
    ) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 200);
        int normalizedOffset = Math.max(offset, 0);
        String searchParam = normalizeSearch(search);

        Page<Document> page = documentRepository.findFiltered(
                searchParam, startDate, endDate,
                PageRequest.of(normalizedOffset / normalizedLimit, normalizedLimit)
        );
        long total = documentRepository.countFiltered(searchParam, startDate, endDate);

        List<TransactionDto> data = page.getContent().stream()
                .map(d -> new TransactionDto(
                        d.getId(),
                        d.getAmount(),
                        d.getRecipient(),
                        d.getDescription(),
                        d.getCategory() != null ? d.getCategory().getName() : null,
                        d.getCreatedAt()
                ))
                .toList();

        return new PagedTransactionsResponse(
                data,
                new PagedTransactionsResponse.Pagination(normalizedLimit, normalizedOffset, total)
        );
    }

    public StatsResponse getStats(String search, OffsetDateTime startDate, OffsetDateTime endDate) {
        StatsRow row = documentRepository.computeStats(normalizeSearch(search), startDate, endDate);

        BigDecimal totalIncome = Objects.requireNonNullElse(row.totalIncome(), BigDecimal.ZERO);
        BigDecimal totalExpense = Objects.requireNonNullElse(row.totalExpense(), BigDecimal.ZERO).abs();
        BigDecimal netTotal = Objects.requireNonNullElse(row.netTotal(), BigDecimal.ZERO);
        long count = row.count() != null ? row.count() : 0L;

        return new StatsResponse(totalIncome, totalExpense, netTotal, count);
    }

    public CategorySummaryResponse getCategorySummary(String search, OffsetDateTime startDate, OffsetDateTime endDate) {
        List<CategorySummaryRow> rows = documentRepository.categorySummary(
                normalizeSearch(search), startDate, endDate
        );

        List<CategorySummaryResponse.Item> items = rows.stream()
                .map(r -> new CategorySummaryResponse.Item(
                        r.categoryName(),
                        Objects.requireNonNullElse(r.total(), BigDecimal.ZERO),
                        r.count() != null ? r.count() : 0L
                ))
                .toList();

        return new CategorySummaryResponse(items);
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : "";
    }
}
