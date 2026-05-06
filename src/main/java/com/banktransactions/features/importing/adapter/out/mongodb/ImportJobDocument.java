package com.banktransactions.features.importing.adapter.out.mongodb;

import com.banktransactions.features.importing.domain.model.ImportStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "import_jobs")
public class ImportJobDocument {

    @Id
    private String id;
    private ImportStatus status;
    @Indexed
    private String fileName;
    private int totalRows;
    private int processedRows;
    private int validRows;
    private int invalidRows;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ErrorDoc> errors;
    private List<String> duplicateTransactionIds;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getProcessedRows() { return processedRows; }
    public void setProcessedRows(int processedRows) { this.processedRows = processedRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ErrorDoc> getErrors() { return errors; }
    public void setErrors(List<ErrorDoc> errors) { this.errors = errors; }

    public List<String> getDuplicateTransactionIds() { return duplicateTransactionIds; }
    public void setDuplicateTransactionIds(List<String> duplicateTransactionIds) { this.duplicateTransactionIds = duplicateTransactionIds; }

    public static class ErrorDoc {
        private int rowNumber;
        private String rawData;
        private String errorMessage;

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

        public String getRawData() { return rawData; }
        public void setRawData(String rawData) { this.rawData = rawData; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
