package com.histr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TransactionDto {
    private UUID id;
    private BigDecimal amount;
    private String recipient;
    private String description;
    private String category;
    private OffsetDateTime createdAt;
}
