package com.banktransactions.features.importing.adapter.out.mongodb;

import com.banktransactions.features.importing.application.port.out.TransactionRepositoryPort;
import com.banktransactions.features.importing.domain.model.Transaction;
import com.banktransactions.features.importing.domain.model.TransactionCategory;
import com.banktransactions.features.importing.domain.valueobject.Iban;
import com.banktransactions.features.importing.domain.valueobject.Money;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class MongoTransactionAdapter implements TransactionRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(MongoTransactionAdapter.class);

    private final TransactionMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoTransactionAdapter(TransactionMongoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Zapisuje transakcje partiami — duplikaty (ten sam transactionId) są pomijane z ostrzeżeniem.
     * Idempotentność: wielokrotny import tego samego pliku nie tworzy duplikatów.
     */
    @Override
    public List<String> saveAll(List<Transaction> transactions) {
        List<String> duplicates = new ArrayList<>();
        for (Transaction tx : transactions) {
            try {
                repository.insert(toDocument(tx));
            } catch (DuplicateKeyException e) {
                log.warn("Duplicate transactionId skipped — id={}, row={}", tx.getId(), tx.getRowNumber());
                duplicates.add(tx.getId());
            }
        }
        return duplicates;
    }

    @Override
    public List<Transaction> findFiltered(String iban, TransactionCategory category, Integer year, Integer month) {
        Query query = new Query();
        if (iban != null && !iban.isBlank()) {
            query.addCriteria(Criteria.where("iban").is(iban.toUpperCase()));
        }
        if (category != null) {
            query.addCriteria(Criteria.where("category").is(category));
        }
        if (year != null && month != null) {
            LocalDate from = LocalDate.of(year, month, 1);
            LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
            query.addCriteria(Criteria.where("transactionDate").gte(from).lte(to));
        } else if (year != null) {
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);
            query.addCriteria(Criteria.where("transactionDate").gte(from).lte(to));
        } else if (month != null) {
            query.addCriteria(Criteria.expr(
                    (AggregationExpression) ctx -> new Document("$eq",
                            List.of(new Document("$month", "$transactionDate"), month))
            ));
        }
        return mongoTemplate.find(query, TransactionDocument.class).stream()
                .map(this::toDomain)
                .toList();
    }

    private TransactionDocument toDocument(Transaction tx) {
        TransactionDocument doc = new TransactionDocument();
        doc.setId(tx.getId());
        doc.setImportJobId(tx.getImportJobId());
        doc.setIban(tx.getIban().getValue());
        doc.setTransactionDate(tx.getTransactionDate());
        doc.setCurrency(tx.getMoney().getCurrency());
        doc.setAmount(tx.getMoney().getAmount());
        doc.setCategory(tx.getCategory());
        doc.setRowNumber(tx.getRowNumber());
        return doc;
    }

    private Transaction toDomain(TransactionDocument doc) {
        return new Transaction(
                doc.getId(),
                doc.getImportJobId(),
                new Iban(doc.getIban()),
                doc.getTransactionDate(),
                new Money(doc.getAmount(), doc.getCurrency()),
                doc.getCategory(),
                doc.getRowNumber());
    }
}
