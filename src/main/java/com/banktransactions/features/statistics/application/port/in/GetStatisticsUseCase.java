package com.banktransactions.features.statistics.application.port.in;

import com.banktransactions.features.statistics.domain.model.GroupBy;
import com.banktransactions.features.statistics.domain.model.StatisticEntry;

import java.time.YearMonth;
import java.util.List;

/**
 * Zwraca zagregowane statystyki transakcji.
 * groupBy: CATEGORY | IBAN | MONTH
 */
public interface GetStatisticsUseCase {
    List<StatisticEntry> getStatistics(GroupBy groupBy, YearMonth from, YearMonth to);
}
