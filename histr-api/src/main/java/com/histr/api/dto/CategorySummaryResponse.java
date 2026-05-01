package com.histr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CategorySummaryResponse {
    private List<Item> data;

    @Data
    @AllArgsConstructor
    public static class Item {
        private String category;
        private BigDecimal total;
        private long count;
    }
}
