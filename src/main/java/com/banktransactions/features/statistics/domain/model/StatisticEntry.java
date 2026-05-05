package com.banktransactions.features.statistics.domain.model;

import java.math.BigDecimal;

public record StatisticEntry(
        String groupKey,
        long transactionCount,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        String currency
) {}
