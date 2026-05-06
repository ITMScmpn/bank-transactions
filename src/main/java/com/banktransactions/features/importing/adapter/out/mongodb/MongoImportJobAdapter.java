package com.banktransactions.features.importing.adapter.out.mongodb;

import com.banktransactions.features.importing.application.port.out.ImportJobRepositoryPort;
import com.banktransactions.features.importing.domain.model.ImportJob;
import com.banktransactions.features.importing.domain.model.ImportStatus;
import com.banktransactions.features.importing.domain.model.TransactionError;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementuje ImportJobRepositoryPort przez MongoDB.
 * Mapuje domenowy ImportJob ↔ ImportJobDocument.
 */
@Component
public class MongoImportJobAdapter implements ImportJobRepositoryPort {

    private final ImportJobMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoImportJobAdapter(ImportJobMongoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ImportJob save(ImportJob job) {
        mongoTemplate.save(toDocument(job));
        return job;
    }

    @Override
    public Optional<ImportJob> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ImportJob> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ImportJobDocument toDocument(ImportJob job) {
        ImportJobDocument doc = new ImportJobDocument();
        doc.setId(job.getId());
        doc.setStatus(job.getStatus());
        doc.setFileName(job.getFileName());
        doc.setTotalRows(job.getTotalRows());
        doc.setProcessedRows(job.getProcessedRows());
        doc.setValidRows(job.getValidRows());
        doc.setInvalidRows(job.getInvalidRows());
        doc.setCreatedAt(job.getCreatedAt());
        doc.setUpdatedAt(job.getUpdatedAt());
        doc.setErrors(job.getErrors().stream().map(e -> {
            ImportJobDocument.ErrorDoc err = new ImportJobDocument.ErrorDoc();
            err.setRowNumber(e.rowNumber());
            err.setRawData(e.rawData());
            err.setErrorMessage(e.errorMessage());
            return err;
        }).toList());
        doc.setDuplicateTransactionIds(new java.util.ArrayList<>(job.getDuplicateTransactionIds()));
        return doc;
    }

    private ImportJob toDomain(ImportJobDocument doc) {
        ImportJob job = new ImportJob(doc.getId(), doc.getFileName(), doc.getCreatedAt());

        job.restoreCounters(
                doc.getTotalRows(), doc.getProcessedRows(),
                doc.getValidRows(), doc.getInvalidRows());

        if (doc.getErrors() != null) {
            doc.getErrors().forEach(e ->
                    job.restoreError(new TransactionError(
                            e.getRowNumber(), e.getRawData(), e.getErrorMessage())));
        }

        if (doc.getDuplicateTransactionIds() != null) {
            doc.getDuplicateTransactionIds().forEach(job::restoreDuplicate);
        }

        restoreStatus(job, doc.getStatus());
        return job;
    }

    private void restoreStatus(ImportJob job, ImportStatus status) {
        // Używamy restoreState dla wszystkich statusów — unikamy ponownego uruchamiania
        // logiki tranzycji (np. complete() przelicza COMPLETED vs COMPLETED_WITH_ERRORS
        // na podstawie bieżącego invalidRows, co mogłoby nadpisać oryginalny status z bazy).
        if (status != ImportStatus.PENDING) {
            job.restoreState(status);
        }
    }
}
