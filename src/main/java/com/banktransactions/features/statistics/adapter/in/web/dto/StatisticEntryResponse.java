package com.banktransactions.features.statistics.adapter.in.web.dto;

import com.banktransactions.features.statistics.domain.model.StatisticEntry;

import java.math.BigDecimal;

public record StatisticEntryResponse(
        String groupKey,
        long transactionCount,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        String currency
) {
    public static StatisticEntryResponse from(StatisticEntry entry) {
        return new StatisticEntryResponse(
                entry.groupKey(), entry.transactionCount(),
                entry.totalAmount(), entry.averageAmount(), entry.currency());
    }
}

