package com.histr.api.service;

import com.histr.api.dto.*;
import com.histr.api.model.Document;
import com.histr.api.model.User;
import com.histr.api.processor.TransactionProcessor;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final TransactionProcessor transactionProcessor;
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

        int processedBatches = 0;
        if (files != null) {
            for (File file : files) {
                List<List<String>> data = parseFile(file.toPath(), file.getName());
                if (!data.isEmpty()) {
                    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    transactionProcessor.processTransactions(user.getId(), data);
                    processedBatches++;
                }
            }
        }

        return Map.of("message", "Documents parsed and processed successfully", "processedBatches", processedBatches);
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
        int processedBatches = 0;
        if (!data.isEmpty()) {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            transactionProcessor.processTransactions(user.getId(), data);
            processedBatches = 1;
        }

        return Map.of(
                "message", "File parsed and processed successfully",
                "fileName", filename,
                "processedBatches", processedBatches
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

    public PagedTransactionsResponse getTransactions(
            int pageNo, int limit, String search,
            Instant startDate, Instant endDate
    ) {
        String searchParam = normalizeSearch(search);
        int sanitizedPageNo = Math.max(pageNo, 0);
        int sanitizedLimit = Math.min(Math.max(limit, 1), 200);

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Page<Document> page = documentRepository.findFiltered(
                searchParam, startDate, endDate, user,
                PageRequest.of(sanitizedPageNo, sanitizedLimit)
        );

        List<TransactionDTO> data = page.getContent().stream()
                .map(d -> new TransactionDTO(
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
                new PagedTransactionsResponse.Pagination(sanitizedLimit, sanitizedPageNo, page.getTotalElements())
        );
    }

    public StatsResponse getStats(String search, OffsetDateTime startDate, OffsetDateTime endDate) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        StatsRow row = documentRepository.computeStats(normalizeSearch(search), startDate, endDate, user);

        BigDecimal totalIncome = Objects.requireNonNullElse(row.totalIncome(), BigDecimal.ZERO);
        BigDecimal totalExpense = Objects.requireNonNullElse(row.totalExpense(), BigDecimal.ZERO).abs();
        BigDecimal netTotal = Objects.requireNonNullElse(row.netTotal(), BigDecimal.ZERO);
        long count = row.count() != null ? row.count() : 0L;

        return new StatsResponse(totalIncome, totalExpense, netTotal, count);
    }

    public CategorySummaryResponse getCategorySummary(String search, Instant startDate, Instant endDate) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<CategorySummaryRow> rows = documentRepository.categorySummary(
                normalizeSearch(search), startDate, endDate, user.getId()
        );

        List<CategorySummaryResponse.Item> items = rows.stream()
                .map(r -> new CategorySummaryResponse.Item(
                        r.getCategoryName(),
                        Objects.requireNonNullElse(r.getTotal(), BigDecimal.ZERO),
                        r.getCount() != null ? r.getCount() : 0L
                ))
                .toList();

        return new CategorySummaryResponse(items);
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : "";
    }
}
