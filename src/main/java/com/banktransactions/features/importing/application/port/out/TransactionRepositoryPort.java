package com.banktransactions.features.importing.application.port.out;

import com.banktransactions.features.importing.domain.model.Transaction;
import com.banktransactions.features.importing.domain.model.TransactionCategory;

import java.util.List;

/** Batch insert, filtrowanie i usuwanie transakcji. Implementacja: MongoTransactionAdapter. */
public interface TransactionRepositoryPort {
    /**
     * Zapisuje transakcje; zwraca listę transactionId, które były duplikatami
     * (już istniały w bazie) i zostały pominięte.
     */
    List<String> saveAll(List<Transaction> transactions);

    /**
     * Wyszukuje transakcje z opcjonalnymi filtrami.
     * Null oznacza brak filtrowania po danym polu.
     */
    List<Transaction> findFiltered(String iban, TransactionCategory category, Integer year, Integer month);
}
