package com.banktransactions.features.importing.adapter.out.mongodb;

import com.banktransactions.features.importing.domain.model.TransactionCategory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "transactions")
public class TransactionDocument {

    @Id
    private String id;
    @Indexed
    private String importJobId;
    @Indexed
    private String iban;
    @Indexed
    private LocalDate transactionDate;
    private String currency;
    @Indexed
    private TransactionCategory category;
    private BigDecimal amount;
    private int rowNumber;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImportJobId() { return importJobId; }
    public void setImportJobId(String importJobId) { this.importJobId = importJobId; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
}
