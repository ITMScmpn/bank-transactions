package com.banktransactions.features.statistics.application.port.out;

import com.banktransactions.features.statistics.domain.model.StatisticEntry;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.List;

/** Agregacja statystyk z bazy danych. Implementacja: MongoStatisticsAdapter. */
public interface StatisticsQueryPort {
    List<StatisticEntry> groupByCategory(@Nullable YearMonth from, @Nullable YearMonth to);
    List<StatisticEntry> groupByIban(@Nullable YearMonth from, @Nullable YearMonth to);
    List<StatisticEntry> groupByMonth(@Nullable YearMonth from, @Nullable YearMonth to);
}

