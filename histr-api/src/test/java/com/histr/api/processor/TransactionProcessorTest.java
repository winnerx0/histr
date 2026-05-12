package com.histr.api.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histr.api.model.Document;
import com.histr.api.model.User;
import com.histr.api.repository.CategoryRepository;
import com.histr.api.repository.DocumentRepository;
import com.histr.api.repository.UserRepository;
import com.histr.api.service.ClassifierService;
import com.histr.api.service.ClassifierService.TransactionClassificationInput;
import com.histr.api.service.ColumnMapperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatModel chatModel;

    @Test
    void parseTransactionsSavesOneDocumentWithValuesFromOneRow() {
        TransactionProcessor processor = new TransactionProcessor(
                new ThrowingClassifierService(),
                new ColumnMapperService(objectMapper, chatModel),
                documentRepository,
                categoryRepository,
                userRepository
        );

        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        List<List<String>> data = List.of(
                List.of("Transaction Date", "Narration", "Amount", "Beneficiary"),
                List.of("2026-05-11", "Transfer to Ada", "2,500.75", "Ada")
        );

        processor.processTransactions("user-1", data);

        List<Document> savedDocuments = captureSavedDocuments();
        assertThat(savedDocuments).hasSize(1);

        Document document = savedDocuments.getFirst();
        assertThat(document.getCreatedAt()).isEqualTo(Instant.parse("2026-05-11T00:00:00Z"));
        assertThat(document.getDescription()).isEqualTo("Transfer to Ada");
        assertThat(document.getAmount()).isEqualByComparingTo(new BigDecimal("2500.75"));
        assertThat(document.getRecipient()).isEqualTo("Ada");
        assertThat(document.getUser()).isSameAs(user);
    }

    @Test
    void parseTransactionsFindsDateHeaderAnywhereInRow() {
        TransactionProcessor processor = new TransactionProcessor(
                new ThrowingClassifierService(),
                new ColumnMapperService(objectMapper, chatModel),
                documentRepository,
                categoryRepository,
                userRepository
        );

        List<List<String>> data = List.of(
                List.of("Account statement"),
                List.of("Trans. Date", "Value Date", "Description", "Debit", "Credit", "Balance After"),
                List.of("21 Dec 2025 02:21:32", "21 Dec 2025",  "Invoice payment", "--", "2.41", "7,021.97")


        );
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        processor.processTransactions("user-1", data);

        List<Document> savedDocuments = captureSavedDocuments();

        Document document = savedDocuments.getFirst();
        assertThat(document.getDescription()).isEqualTo("Invoice payment");
        assertThat(document.getRecipient()).isEqualTo(null);
        assertThat(document.getAmount()).isEqualByComparingTo(new BigDecimal("2.41"));
        assertThat(document.getCreatedAt()).isEqualTo(Instant.parse("2025-12-21T00:00:00Z"));
        assertThat(document.getUser()).isSameAs(user);
    }

    @Test
    public void parseCurrencyWithDecimalPlaceSuccessfully(){

        TransactionProcessor processor = new TransactionProcessor(
                new ThrowingClassifierService(),
                new ColumnMapperService(objectMapper, chatModel),
                documentRepository,
                categoryRepository,
                userRepository
        );

        BigDecimal value = processor.parseCurrency("₦2.41");

        assertThat(value).isEqualTo("2.41");
    }

    private List<Document> captureSavedDocuments() {
        ArgumentCaptor<Iterable<Document>> documents = ArgumentCaptor.forClass(Iterable.class);
        verify(documentRepository).saveAll(documents.capture());

        List<Document> savedDocuments = new ArrayList<>();
        documents.getValue().forEach(savedDocuments::add);
        return savedDocuments;
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
