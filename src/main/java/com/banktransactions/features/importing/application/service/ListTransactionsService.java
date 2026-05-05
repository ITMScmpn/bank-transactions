package com.banktransactions.features.importing.application.service;

import com.banktransactions.features.importing.application.port.in.ListTransactionsUseCase;
import com.banktransactions.features.importing.application.port.out.TransactionRepositoryPort;
import com.banktransactions.features.importing.domain.model.Transaction;
import com.banktransactions.features.importing.domain.model.TransactionCategory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTransactionsService implements ListTransactionsUseCase {

    private final TransactionRepositoryPort transactionRepository;

    public ListTransactionsService(TransactionRepositoryPort transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<Transaction> findFiltered(String iban, TransactionCategory category, Integer year, Integer month) {
        return transactionRepository.findFiltered(iban, category, year, month);
    }
}

