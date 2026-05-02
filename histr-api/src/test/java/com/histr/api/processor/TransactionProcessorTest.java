package com.histr.api.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histr.api.model.Document;
import com.histr.api.repository.CategoryRepository;
import com.histr.api.repository.DocumentRepository;
import com.histr.api.service.ClassifierService;
import com.histr.api.service.ClassifierService.TransactionClassificationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseTransactionsFindsDateHeaderAnywhereInRow() {
        TestStringRedisTemplate redis = new TestStringRedisTemplate(valueOperations);
        TransactionProcessor processor = new TransactionProcessor(
                redis,
                objectMapper,
                new ThrowingClassifierService(),
                documentRepository,
                categoryRepository
        );
        ReflectionTestUtils.setField(processor, "processedCountKey", "worker:processed_count");

        List<List<String>> data = List.of(
                List.of("Account statement"),
                List.of("Description", "Date", "Credit", "Payee"),
                List.of("Invoice payment", "2026-04-30", "125.50", "Acme")
        );

        ReflectionTestUtils.invokeMethod(processor, "parseTransactions", data);

        ArgumentCaptor<Iterable<Document>> documents = ArgumentCaptor.forClass(Iterable.class);
        verify(documentRepository).saveAll(documents.capture());
        verify(valueOperations).increment(eq("worker:processed_count"), eq(1L));

        List<Document> savedDocuments = new ArrayList<>();
        documents.getValue().forEach(savedDocuments::add);
        Document document = savedDocuments.getFirst();
        assertThat(document.getDescription()).isEqualTo("Invoice payment");
        assertThat(document.getRecipient()).isEqualTo("Acme");
        assertThat(document.getAmount()).isEqualByComparingTo(new BigDecimal("125.50"));
        assertThat(document.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-04-30T00:00:00Z"));
    }

    private static class TestStringRedisTemplate extends StringRedisTemplate {
        private final ValueOperations<String, String> valueOperations;

        private TestStringRedisTemplate(ValueOperations<String, String> valueOperations) {
            this.valueOperations = valueOperations;
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOperations;
        }
    }

    private static class ThrowingClassifierService extends ClassifierService {
        private ThrowingClassifierService() {
            super(null, null);
        }

        @Override
        public List<UUID> classifyTransactions(List<TransactionClassificationInput> transactions) {
            throw new RuntimeException("classification unavailable");
        }
    }
}
