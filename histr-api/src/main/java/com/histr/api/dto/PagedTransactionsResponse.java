package com.histr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedTransactionsResponse {
    private List<TransactionDto> data;
    private Pagination pagination;

    @Data
    @AllArgsConstructor
    public static class Pagination {
        private int limit;
        private int offset;
        private long total;
    }
}
