package com.histr.api.processor;

import com.histr.api.model.Document;
import com.histr.api.model.User;
import com.histr.api.repository.CategoryRepository;
import com.histr.api.repository.DocumentRepository;
import com.histr.api.repository.UserRepository;
import com.histr.api.service.ClassifierService;
import com.histr.api.service.ClassifierService.TransactionClassificationInput;
import com.histr.api.service.ColumnMapperService;
import com.histr.api.service.ColumnMapperService.ColumnMapping;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProcessor {

    private static final Pattern TRANSFER_RECIPIENT = Pattern.compile(
            "(?i)^\\s*transfer\\s+(?:from|to)\\s+(.+?)\\s*$");
    private static final Pattern PIPE_RECIPIENT = Pattern.compile(
            "^[^|]+\\|[^|]+\\|\\s*([^|]+?)\\s*$");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            // numeric, 4-digit year
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            // numeric, 2-digit year
            DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yy", Locale.ENGLISH),
            // alpha month
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH),
            // with time, 4-digit year
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss", Locale.ENGLISH),
            // with time, 2-digit year
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yy HH:mm:ss", Locale.ENGLISH)
    );

    private final ClassifierService classifierService;
    private final ColumnMapperService columnMapperService;
    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public void processTransactions(String userId, List<List<String>> data) {
        parseTransactions(userId, data);
    }

    private void parseTransactions(String userId, List<List<String>> data) {

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        log.info("Parsing now");
        List<Document> documents = new ArrayList<>();

        ColumnMapping mapping = columnMapperService.detect(data);
        log.info("Column mapping: {}", mapping);
        if (!mapping.hasMinimumFields()) return;

        int start = Math.max(0, mapping.headerRowIndex() + 1);
        for (int i = start; i < data.size(); i++) {
            List<String> row = data.get(i);
            if (row == null || row.isEmpty()) continue;

            Document doc = new Document();
            doc.setAmount(BigDecimal.ZERO);
            doc.setDescription("");
            doc.setCreatedAt(Instant.now());
            doc.setUser(user);

            String dateVal = cell(row, mapping.dateCol());
            if (!dateVal.isBlank()) {
                Instant parsed = parseDate(dateVal);
                if (parsed != null) doc.setCreatedAt(parsed);
            }

            doc.setDescription(cell(row, mapping.descriptionCol()));

            if (mapping.amountCol() >= 0) {
                String v = cell(row, mapping.amountCol());
                if (!v.isBlank() && !v.equals("--")) doc.setAmount(parseSignedCurrency(v));
            } else {
                String credit = cell(row, mapping.creditCol());
                String debit = cell(row, mapping.debitCol());
                if (!credit.isBlank() && !credit.equals("--")) {
                    doc.setAmount(parseCurrency(credit));
                } else if (!debit.isBlank() && !debit.equals("--")) {
                    doc.setAmount(parseCurrency(debit).negate());
                }
                log.info("document {} {}", doc.getCreatedAt(), doc.getAmount());
            }

            String recipient = cell(row, mapping.recipientCol());
            if (!recipient.isBlank()) doc.setRecipient(recipient);

            if ((doc.getRecipient() == null || doc.getRecipient().isBlank())
                    && !doc.getDescription().isBlank()) {
                String extracted = extractRecipient(doc.getDescription());
                if (extracted != null) doc.setRecipient(extracted);
            }

            if (doc.getDescription().isBlank() && (doc.getRecipient() == null || doc.getRecipient().isBlank())) {
                continue;
            }
            if (doc.getAmount() == null || doc.getAmount().signum() == 0) {
                // skip rows where no amount could be extracted
                log.info("this row skipped {}", doc);
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
        log.info("Processed {} transactions", documents.size());
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, fmt).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
            try {
                return LocalDateTime.parse(value, fmt).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private String cell(List<String> row, int col) {
        if (col < 0 || col >= row.size()) return "";
        String v = row.get(col);
        return v == null ? "" : v.trim();
    }

    private String extractRecipient(String description) {
        if (description == null) return null;
        String d = description.trim();
        if (d.isEmpty()) return null;

        Matcher m = TRANSFER_RECIPIENT.matcher(d);
        if (m.matches()) return m.group(1).trim();

        Matcher p = PIPE_RECIPIENT.matcher(d);
        if (p.matches()) return p.group(1).trim();

        return null;
    }

    public BigDecimal parseCurrency(String value) {
        try {
            return new BigDecimal(value.replace("₦", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseSignedCurrency(String value) {
        String v = value.replace("₦", "").replace(",", "").trim();
        boolean negative = false;
        if (v.startsWith("(") && v.endsWith(")")) {
            negative = true;
            v = v.substring(1, v.length() - 1);
        } else if (v.endsWith("Dr") || v.endsWith("DR")) {
            negative = true;
            v = v.substring(0, v.length() - 2).trim();
        } else if (v.endsWith("Cr") || v.endsWith("CR")) {
            v = v.substring(0, v.length() - 2).trim();
        }
        try {
            BigDecimal n = new BigDecimal(v);
            return negative ? n.negate() : n;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
