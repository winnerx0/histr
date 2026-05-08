package com.histr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedTransactionsResponse {
    private List<TransactionDTO> data;
    private Pagination pagination;

    @Data
    @AllArgsConstructor
    public static class Pagination {
        private int limit;
        private int pageNo;
        private long total;
    }
}
