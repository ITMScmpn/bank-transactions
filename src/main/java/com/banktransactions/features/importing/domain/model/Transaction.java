package com.banktransactions.features.importing.domain.model;

import com.banktransactions.features.importing.domain.valueobject.Iban;
import com.banktransactions.features.importing.domain.valueobject.Money;

import java.time.LocalDate;

/**
 * Agregat domenowy reprezentujący pojedynczą transakcję bankową.
 * Używa Value Objects (Iban, Money) gwarantujących poprawność danych.
 */
public class Transaction {

    private final String id;
    private final String importJobId;
    private final Iban iban;
    private final LocalDate transactionDate;
    private final Money money;
    private final TransactionCategory category;
    private final int rowNumber;

    public Transaction(String id, String importJobId, Iban iban, LocalDate transactionDate,
                       Money money, TransactionCategory category, int rowNumber) {
        this.id = id;
        this.importJobId = importJobId;
        this.iban = iban;
        this.transactionDate = transactionDate;
        this.money = money;
        this.category = category;
        this.rowNumber = rowNumber;
    }

    public String getId() { return id; }
    public String getImportJobId() { return importJobId; }
    public Iban getIban() { return iban; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public Money getMoney() { return money; }
    public TransactionCategory getCategory() { return category; }
    public int getRowNumber() { return rowNumber; }
}
