package com.banktransactions.features.statistics.application.service;

import com.banktransactions.features.statistics.application.port.in.GetStatisticsUseCase;
import com.banktransactions.features.statistics.application.port.out.StatisticsQueryPort;
import com.banktransactions.features.statistics.domain.model.GroupBy;
import com.banktransactions.features.statistics.domain.model.StatisticEntry;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
public class GetStatisticsService implements GetStatisticsUseCase {

    private final StatisticsQueryPort statisticsQuery;

    public GetStatisticsService(StatisticsQueryPort statisticsQuery) {
        this.statisticsQuery = statisticsQuery;
    }

    @Override
    public List<StatisticEntry> getStatistics(GroupBy groupBy, YearMonth from, YearMonth to) {
        return switch (groupBy) {
            case CATEGORY -> statisticsQuery.groupByCategory(from, to);
            case IBAN     -> statisticsQuery.groupByIban(from, to);
            case MONTH    -> statisticsQuery.groupByMonth(from, to);
        };
    }
}