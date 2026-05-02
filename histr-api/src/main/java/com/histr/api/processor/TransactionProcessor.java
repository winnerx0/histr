package com.histr.api.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histr.api.model.Document;
import com.histr.api.repository.CategoryRepository;
import com.histr.api.repository.DocumentRepository;
import com.histr.api.service.ClassifierService;
import com.histr.api.service.ClassifierService.TransactionClassificationInput;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProcessor {

    private static final String QUEUE = "transactions";

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    );

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ClassifierService classifierService;
    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.worker-heartbeat-key:worker:heartbeat}")
    private String heartbeatKey;

    @Value("${app.worker-processed-count-key:worker:processed_count}")
    private String processedCountKey;

    private volatile boolean running = true;
    private Thread workerThread;

    @PostConstruct
    void start() {
        workerThread = Thread.ofVirtual().name("transaction-worker").start(this::processLoop);
        log.info("Transaction processor started");
    }

    @PreDestroy
    void stop() {
        log.info("Transaction processor stopped");
        running = false;
        if (workerThread != null) workerThread.interrupt();
    }

    @Scheduled(fixedDelay = 5000)
    void heartbeat() {
        redis.opsForValue().set(heartbeatKey, Instant.now().toString());
    }

    private void processLoop() {
        while (running) {
            try {
                log.info("Process loop running");
                // BLPOP with 5s timeout so the thread can check 'running' on shutdown
                String json = redis.opsForList().leftPop(QUEUE, 5, TimeUnit.SECONDS);
                if (json == null) continue;

                List<List<String>> data = objectMapper.readValue(json, new TypeReference<>() {});
                parseTransactions(data);

                heartbeat();
            } catch (Exception e) {
                log.error("Error processing transaction batch", e);
            }
        }
    }

    private void parseTransactions(List<List<String>> data) {

        log.info("Parsing now");
        List<Document> documents = new ArrayList<>();

        int headerRowIndex = -1;
        for (int i = 0; i < data.size(); i++) {
            List<String> row = data.get(i);
            log.info("data {}", row);
            if (firstDateHeaderIndex(row) != -1) {
                headerRowIndex = i;
                break;
            }
        }

        log.info("parse {}", headerRowIndex);
        if (headerRowIndex == -1) return;

        List<String> headers = data.get(headerRowIndex);

        for (int i = headerRowIndex + 1; i < data.size(); i++) {
            List<String> row = data.get(i);
            if (row.isEmpty()) continue;

            Document doc = new Document();
            doc.setAmount(BigDecimal.ZERO);
            doc.setDescription("");
            doc.setCreatedAt(OffsetDateTime.now());

            for (int k = 0; k < headers.size() && k < row.size(); k++) {
                String key = headers.get(k);
                String value = row.get(k);
                if (key == null || key.isBlank()) continue;
                String lk = key.toLowerCase();
                String v = value != null ? value.trim() : "";

                if (lk.contains("date")) {
                    OffsetDateTime parsed = parseDate(v);
                    if (parsed != null) doc.setCreatedAt(parsed);
                } else if (lk.contains("description")) {
                    doc.setDescription(v);
                } else if (lk.contains("credit") || lk.contains("money in")) {
                    if (!v.isEmpty() && !v.equals("--")) {
                        doc.setAmount(parseCurrency(v));
                    }
                } else if (lk.contains("debit") || lk.contains("money out")) {
                    if (!v.isEmpty() && !v.equals("--")) {
                        doc.setAmount(parseCurrency(v).negate());
                    }
                } else if (lk.contains("receipient") || lk.contains("payee")
                        || lk.contains("beneficiary") || lk.contains("to / from")) {
                    doc.setRecipient(v);
                }
            }

            if (doc.getDescription().isBlank() && (doc.getRecipient() == null || doc.getRecipient().isBlank())) {
                continue;
            }

            documents.add(doc);
        }

        if (documents.isEmpty()) return;

        try {
            List<TransactionClassificationInput> classificationInputs = documents.stream()
                    .map(doc -> new TransactionClassificationInput(
                            doc.getDescription(),
                            doc.getRecipient(),
                            doc.getAmount().doubleValue()
                    ))
                    .toList();
            List<UUID> categoryIds = classifierService.classifyTransactions(classificationInputs);
            for (int i = 0; i < documents.size(); i++) {
                documents.get(i).setCategory(categoryRepository.getReferenceById(categoryIds.get(i)));
            }
        } catch (Exception e) {
            log.warn("Batch classification failed: {}", e.getMessage());
        }

        documentRepository.saveAll(documents);
        redis.opsForValue().increment(processedCountKey, documents.size());
        log.info("Processed {} transactions", documents.size());
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, fmt).atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
            try {
                return LocalDateTime.parse(value, fmt).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private int firstDateHeaderIndex(List<String> row) {
        if (row == null || row.isEmpty()) return -1;

        for (int i = 0; i < row.size(); i++) {
            String cell = row.get(i);
            if (cell != null && cell.toLowerCase().contains("date")) {
                return i;
            }
        }

        return -1;
    }

    private BigDecimal parseCurrency(String value) {
        try {
            return new BigDecimal(value.replace("₦", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
