package com.banktransactions.features.statistics.adapter.out.mongodb;

import com.banktransactions.features.statistics.application.port.out.StatisticsQueryPort;
import com.banktransactions.features.statistics.domain.model.StatisticEntry;
import org.bson.Document;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementuje StatisticsQueryPort przez MongoDB Aggregation Pipeline.
 * Czyta z kolekcji "transactions" zapisanej przez feature importing.
 */
@Component
public class MongoStatisticsAdapter implements StatisticsQueryPort {

    private static final String COLLECTION = "transactions";
    private final MongoTemplate mongoTemplate;

    public MongoStatisticsAdapter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<StatisticEntry> groupByCategory(@Nullable YearMonth from, @Nullable YearMonth to) {
        return aggregateByField("category", from, to);
    }

    @Override
    public List<StatisticEntry> groupByIban(@Nullable YearMonth from, @Nullable YearMonth to) {
        return aggregateByField("iban", from, to);
    }

    @Override
    public List<StatisticEntry> groupByMonth(@Nullable YearMonth from, @Nullable YearMonth to) {
        return aggregateByMonth(from, to);
    }

    private List<StatisticEntry> aggregateByField(String field, @Nullable YearMonth from, @Nullable YearMonth to) {
        List<AggregationOperation> ops = new ArrayList<>();
        buildDateFilter(from, to).ifPresent(ops::add);
        ops.add(Aggregation.group(field)
                .count().as("transactionCount")
                .sum("amount").as("totalAmount")
                .avg("amount").as("averageAmount")
                .first("currency").as("currency"));
        ops.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "totalAmount")));
        return runAndMap(ops);
    }

    private List<StatisticEntry> aggregateByMonth(@Nullable YearMonth from, @Nullable YearMonth to) {
        List<AggregationOperation> ops = new ArrayList<>();
        buildDateFilter(from, to).ifPresent(ops::add);
        // wyciągnięcie "yyyy-MM" ze stringa daty (LocalDate serializowany jako string w Mongo)
        ops.add(ctx -> new Document("$addFields",
                new Document("yearMonth",
                        new Document("$substr", List.of(
                                new Document("$toString", "$transactionDate"), 0, 7)))));
        ops.add(Aggregation.group("yearMonth")
                .count().as("transactionCount")
                .sum("amount").as("totalAmount")
                .avg("amount").as("averageAmount")
                .first("currency").as("currency"));
        ops.add(Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id")));
        return runAndMap(ops);
    }

    private Optional<MatchOperation> buildDateFilter(@Nullable YearMonth from, @Nullable YearMonth to) {
        if (from == null && to == null) return Optional.empty();
        Criteria criteria = Criteria.where("transactionDate");
        if (from != null) criteria.gte(from.atDay(1));
        if (to != null)   criteria.lte(to.atEndOfMonth());
        return Optional.of(Aggregation.match(criteria));
    }

    private List<StatisticEntry> runAndMap(List<AggregationOperation> ops) {
        return mongoTemplate
                .aggregate(Aggregation.newAggregation(ops), COLLECTION, Map.class)
                .getMappedResults()
                .stream()
                .map(this::toEntry)
                .toList();
    }

    private StatisticEntry toEntry(Map<?, ?> row) {
        return new StatisticEntry(
                String.valueOf(row.get("_id")),
                ((Number) row.get("transactionCount")).longValue(),
                toBd(row.get("totalAmount")),
                toBd(row.get("averageAmount")),
                row.get("currency") != null ? String.valueOf(row.get("currency")) : "N/A"
        );
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
