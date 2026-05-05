package com.banktransactions.features.importing.adapter.in.web.dto;

import com.banktransactions.features.importing.domain.model.Transaction;
import com.banktransactions.features.importing.domain.model.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        String id,
        String importJobId,
        String iban,
        LocalDate transactionDate,
        String currency,
        BigDecimal amount,
        TransactionCategory category
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getImportJobId(),
                tx.getIban().getValue(),
                tx.getTransactionDate(),
                tx.getMoney().getCurrency(),
                tx.getMoney().getAmount(),
                tx.getCategory());
    }
}

