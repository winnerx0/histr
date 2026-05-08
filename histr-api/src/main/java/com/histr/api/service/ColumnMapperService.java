package com.histr.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detects the role of each column in a tabular bank/wallet statement.
 * Tries deterministic heuristics first; falls back to OpenAI when confidence is low.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnMapperService {

    public record ColumnMapping(
            int headerRowIndex,
            int dateCol,
            int descriptionCol,
            int debitCol,
            int creditCol,
            int amountCol,
            int recipientCol
    ) {
        public static ColumnMapping empty() {
            return new ColumnMapping(-1, -1, -1, -1, -1, -1, -1);
        }

        public boolean hasMinimumFields() {
            return dateCol >= 0 && (debitCol >= 0 || creditCol >= 0 || amountCol >= 0);
        }
    }

    private static final List<DateTimeFormatter> DATE_PROBES = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    );

    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;

    public ColumnMapping detect(List<List<String>> data) {
        ColumnMapping heuristic = detectHeuristic(data);
        if (heuristic.hasMinimumFields()) {
            log.debug("Heuristic column mapping succeeded: {}", heuristic);
            return heuristic;
        }
        log.info("Heuristic column mapping insufficient ({}), falling back to LLM", heuristic);
        try {
            ColumnMapping llm = detectWithLLM(data);
            if (llm.hasMinimumFields()) return llm;
            log.warn("LLM mapping also insufficient: {}", llm);
            return llm;
        } catch (Exception e) {
            log.warn("LLM column mapping failed: {}", e.getMessage());
            return heuristic;
        }
    }

    /* ------------------------ heuristic ------------------------ */

    private ColumnMapping detectHeuristic(List<List<String>> data) {
        for (int i = 0; i < data.size(); i++) {
            List<String> row = data.get(i);
            if (row == null || row.size() < 3) continue;

            int date = -1, desc = -1, debit = -1, credit = -1, amount = -1, recipient = -1;

            for (int c = 0; c < row.size(); c++) {
                String raw = row.get(c);
                if (raw == null) continue;
                String cell = raw.toLowerCase().trim();
                if (cell.isBlank() || cell.length() > 50) continue;

                // Skip "value date" so transaction date wins.
                if (cell.contains("date") && !cell.contains("value") && date == -1) date = c;
                else if (cell.contains("date") && date == -1) date = c; // accept value date if nothing else
                else if ((cell.contains("description") || cell.contains("narration")
                        || cell.contains("particulars") || cell.contains("details")
                        || cell.contains("remark")) && desc == -1) desc = c;
                else if (cell.contains("debit") && !cell.contains("count") && debit == -1) debit = c;
                else if ((cell.contains("money out") || cell.contains("withdrawal") || cell.equals("dr")) && debit == -1) debit = c;
                else if (cell.contains("credit") && !cell.contains("count") && credit == -1) credit = c;
                else if ((cell.contains("money in") || cell.contains("deposit") || cell.equals("cr")) && credit == -1) credit = c;
                else if ((cell.equals("amount") || cell.startsWith("amount ") || cell.contains("amount(")) && amount == -1) amount = c;
                else if ((cell.contains("to / from") || cell.contains("payee") || cell.contains("beneficiary")
                        || cell.contains("recipient") || cell.contains("receipient")
                        || cell.contains("counterparty")) && recipient == -1) recipient = c;
            }

            if (date == -1) continue;
            if (debit == -1 && credit == -1 && amount == -1) continue;

            // Confirm date column actually parses on the next non-empty row.
            if (!confirmDateColumn(data, i, date)) continue;
            return new ColumnMapping(i, date, desc, debit, credit, amount, recipient);
        }
        return ColumnMapping.empty();
    }

    private boolean confirmDateColumn(List<List<String>> data, int headerIdx, int dateCol) {
        for (int j = headerIdx + 1; j < Math.min(headerIdx + 6, data.size()); j++) {
            List<String> next = data.get(j);
            if (next == null || next.size() <= dateCol) continue;
            String v = next.get(dateCol);
            if (v == null || v.isBlank()) continue;
            return looksLikeDate(v.trim());
        }
        return false;
    }

    private boolean looksLikeDate(String v) {
        for (DateTimeFormatter fmt : DATE_PROBES) {
            try { LocalDate.parse(v, fmt); return true; } catch (DateTimeParseException ignored) {}
            try { LocalDateTime.parse(v, fmt); return true; } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    /* ------------------------ LLM fallback ------------------------ */

    private ColumnMapping detectWithLLM(List<List<String>> data) throws Exception {
        String sample = buildSample(data);

        String system = """
                You map bank/wallet statement columns to known fields.
                Return JSON only, matching this schema:
                {
                  "headerRowIndex": int,        // 0-based index of the header row, or -1 if there is no header
                  "dateCol": int,               // -1 if absent
                  "descriptionCol": int,        // -1 if absent
                  "debitCol": int,              // money-out column; -1 if absent
                  "creditCol": int,             // money-in column; -1 if absent
                  "amountCol": int,             // single signed-amount column; -1 if absent
                  "recipientCol": int           // payee / to-from column; -1 if absent
                }
                Rules:
                - If the statement uses a single signed Amount column, set amountCol and leave debit/credit at -1.
                - If it has separate Debit/Credit (or Money In/Out), set those and leave amountCol at -1.
                - Prefer transaction date over value/posting date when both exist.
                - Indices are 0-based positions within a row.
                - Output JSON only. No prose, no markdown fences.
                """;

        String user = "Sample rows (objects with row index `i` and cells `r`):\n" + sample;

        String response = ChatClient.create(chatModel)
                .prompt()
                .system(system)
                .user(user)
                .call()
                .content();

        String json = stripFences(response);
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        return new ColumnMapping(
                intOrNeg(parsed.get("headerRowIndex")),
                intOrNeg(parsed.get("dateCol")),
                intOrNeg(parsed.get("descriptionCol")),
                intOrNeg(parsed.get("debitCol")),
                intOrNeg(parsed.get("creditCol")),
                intOrNeg(parsed.get("amountCol")),
                intOrNeg(parsed.get("recipientCol"))
        );
    }

    private String buildSample(List<List<String>> data) throws Exception {
        int headCount = Math.min(25, data.size());
        int tailStart = Math.max(headCount, data.size() - 5);

        List<Map<String, Object>> sample = new ArrayList<>();
        for (int i = 0; i < headCount; i++) {
            sample.add(rowEntry(i, data.get(i)));
        }
        for (int i = tailStart; i < data.size(); i++) {
            sample.add(rowEntry(i, data.get(i)));
        }
        return objectMapper.writeValueAsString(sample);
    }

    private Map<String, Object> rowEntry(int i, List<String> row) {
        Map<String, Object> m = new HashMap<>();
        m.put("i", i);
        m.put("r", row == null ? List.of() : row);
        return m;
    }

    private String stripFences(String s) {
        if (s == null) return "{}";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    private int intOrNeg(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }
}
