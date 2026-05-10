package com.histr.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histr.api.dto.CategorySummaryResponse;
import com.histr.api.dto.CategorySummaryRow;
import com.histr.api.dto.PagedTransactionsResponse;
import com.histr.api.dto.StatsResponse;
import com.histr.api.dto.StatsRow;
import com.histr.api.enums.Role;
import com.histr.api.model.Category;
import com.histr.api.model.Document;
import com.histr.api.model.User;
import com.histr.api.repository.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final String TRANSACTIONS_QUEUE = "transactions";

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private DocumentRepository documentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TestStringRedisTemplate redis;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        redis = new TestStringRedisTemplate(listOperations);
        documentService = new DocumentService(redis, objectMapper, documentRepository);
        ReflectionTestUtils.setField(documentService, "maxUploadSizeMb", 15L);

        User user = new User();
        user.setId("user-1");
        user.setUsername("tester");
        user.setEmail("tester@example.com");
        user.setRole(Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void parseUploadedFileParsesCsvAndPushesRowsToRedis() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                "recipient,amount,description\nAcme,125.50,Invoice payment\n".getBytes()
        );

        Map<String, Object> result = documentService.parseUploadedFile(file);

        assertThat(result)
                .containsEntry("message", "File parsed and queued for processing")
                .containsEntry("fileName", "transactions.csv")
                .containsEntry("queuedBatches", 1);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(listOperations).leftPush(eq(TRANSACTIONS_QUEUE), payload.capture());

        JsonNode queued = objectMapper.readTree(payload.getValue());
        assertThat(queued.has("user-1")).isTrue();

        List<List<String>> rows = objectMapper.convertValue(
                queued.get("user-1"),
                objectMapper.getTypeFactory().constructCollectionType(
                        List.class,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                )
        );
        assertThat(rows).containsExactly(
                List.of("recipient", "amount", "description"),
                List.of("Acme", "125.50", "Invoice payment")
        );
    }

    @Test
    void parseUploadedFileRejectsUnsupportedFileTypes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.txt",
                "text/plain",
                "not,csv".getBytes()
        );

        assertThatThrownBy(() -> documentService.parseUploadedFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only .xlsx and .csv files are supported");

        assertThat(redis.opsForListCalled).isFalse();
    }

    @Test
    void getTransactionsNormalizesPagingSearchAndMapsDocuments() {
        Instant createdAt = Instant.parse("2026-04-30T12:00:00Z");
        Category category = new Category();
        category.setName("Income");

        Document document = new Document();
        UUID id = UUID.randomUUID();
        document.setId(id);
        document.setAmount(new BigDecimal("125.50"));
        document.setRecipient("Acme");
        document.setDescription("Invoice payment");
        document.setCategory(category);
        document.setCreatedAt(createdAt);

        when(documentRepository.findFiltered(eq("invoice"), eq(null), eq(null), any(User.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(document)));

        PagedTransactionsResponse response = documentService.getTransactions(-5, 500, " invoice ", null, null);

        assertThat(response.getPagination().getLimit()).isEqualTo(200);
        assertThat(response.getPagination().getPageNo()).isZero();
        assertThat(response.getPagination().getTotal()).isEqualTo(1L);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().getId()).isEqualTo(id);
        assertThat(response.getData().getFirst().getCategory()).isEqualTo("Income");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(documentRepository).findFiltered(eq("invoice"), eq(null), eq(null), any(User.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void getStatsDefaultsNullAggregateValues() {
        when(documentRepository.computeStats(eq(""), isNull(), isNull(), any(User.class)))
                .thenReturn(new StatsRow(null, null, null, null));

        StatsResponse response = documentService.getStats("   ", null, null);

        assertThat(response.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getNetTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTransactionCount()).isZero();
    }

//    @Test
//    void getCategorySummaryDefaultsNullTotalsAndCounts() {
//        when(documentRepository.categorySummary("food", null, null))
//                .thenReturn(List.of(new CategorySummaryRow("Groceries", null, null)));
//
//        CategorySummaryResponse response = documentService.getCategorySummary(" food ", null, null);
//
//        assertThat(response.getData()).hasSize(1);
//        assertThat(response.getData().getFirst().getCategory()).isEqualTo("Groceries");
//        assertThat(response.getData().getFirst().getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
//        assertThat(response.getData().getFirst().getCount()).isZero();
//    }

    private static class TestStringRedisTemplate extends StringRedisTemplate {
        private final ListOperations<String, String> listOperations;
        private boolean opsForListCalled;

        private TestStringRedisTemplate(ListOperations<String, String> listOperations) {
            this.listOperations = listOperations;
        }

        @Override
        public ListOperations<String, String> opsForList() {
            opsForListCalled = true;
            return listOperations;
        }
    }
}
