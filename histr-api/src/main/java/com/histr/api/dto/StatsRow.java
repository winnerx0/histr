package com.histr.api.dto;

import java.math.BigDecimal;

public record StatsRow(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netTotal,
        Long count
) {}
