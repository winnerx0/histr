package com.histr.api.dto;

import java.math.BigDecimal;

public record CategorySummaryRow(
        String categoryName,
        BigDecimal total,
        Long count
) {}
