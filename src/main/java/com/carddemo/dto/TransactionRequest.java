package com.carddemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private Long accountId;
    private BigDecimal amount;
    private String transactionType;
    private String description;
    private String merchantName;
    private String merchantCategory;
}
