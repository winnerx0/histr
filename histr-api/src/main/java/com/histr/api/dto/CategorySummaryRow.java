package com.histr.api.dto;

import java.math.BigDecimal;

public interface CategorySummaryRow {
        String getCategoryName();
        BigDecimal getTotal();
        Long getCount();
}
