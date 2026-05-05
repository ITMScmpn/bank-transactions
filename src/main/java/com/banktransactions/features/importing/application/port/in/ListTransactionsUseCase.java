package com.banktransactions.features.importing.application.port.in;

import com.banktransactions.features.importing.domain.model.Transaction;
import com.banktransactions.features.importing.domain.model.TransactionCategory;

import java.util.List;

public interface ListTransactionsUseCase {
    List<Transaction> findFiltered(String iban, TransactionCategory category, Integer year, Integer month);
}

