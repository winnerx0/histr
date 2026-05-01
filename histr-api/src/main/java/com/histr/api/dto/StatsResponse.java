package com.histr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class StatsResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netTotal;
    private long transactionCount;
}
