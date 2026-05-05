package com.banktransactions.features.importing.application.service;

import com.banktransactions.features.importing.application.port.out.CsvReaderPort;
import com.banktransactions.features.importing.application.port.out.CsvReaderPort.ParsedTransactionRow;
import com.banktransactions.features.importing.application.port.out.ImportJobRepositoryPort;
import com.banktransactions.features.importing.application.port.out.TransactionRepositoryPort;
import com.banktransactions.features.importing.domain.model.ImportJob;
import com.banktransactions.features.importing.domain.model.Transaction;
import com.banktransactions.features.importing.domain.model.TransactionCategory;
import com.banktransactions.features.importing.domain.model.TransactionError;
import com.banktransactions.features.importing.domain.service.TransactionValidator;
import com.banktransactions.features.importing.domain.valueobject.Iban;
import com.banktransactions.features.importing.domain.valueobject.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orkiestruje przetwarzanie importu:
 * 1. Pobiera job → ustawia PROCESSING
 * 2. Czyta CSV przez CsvReaderPort
 * 3. Waliduje domenowo każdy wiersz (TransactionValidator)
 * 4. Zapisuje poprawne rekordy w partiach (BATCH_SIZE = 500)
 * 5. Ustawia końcowy status
 */
@Service
public class ProcessImportJobService {

    private static final Logger log = LoggerFactory.getLogger(ProcessImportJobService.class);
    private static final int BATCH_SIZE = 500;

    private final ImportJobRepositoryPort importJobRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final CsvReaderPort csvReader;
    private final TransactionValidator validator;

    public ProcessImportJobService(ImportJobRepositoryPort importJobRepository,
                                   TransactionRepositoryPort transactionRepository,
                                   CsvReaderPort csvReader,
                                   TransactionValidator validator) {
        this.importJobRepository = importJobRepository;
        this.transactionRepository = transactionRepository;
        this.csvReader = csvReader;
        this.validator = validator;
    }

    public void process(String jobId, InputStream csvInputStream) {
        ImportJob job = null;
        try {
            job = importJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalStateException("ImportJob not found: " + jobId));

            job.startProcessing();
            importJobRepository.save(job);
            log.info("Processing started — jobId={}, file={}", jobId, job.getFileName());

            List<ParsedTransactionRow> rows = csvReader.read(csvInputStream);
            job.initializeTotalRows(rows.size());

            List<Transaction> batch = new ArrayList<>();

            for (ParsedTransactionRow row : rows) {
                try {
                    batch.add(parseAndValidate(row, jobId));
                    job.recordValid();

                    if (batch.size() >= BATCH_SIZE) {
                        transactionRepository.saveAll(new ArrayList<>(batch))
                                .forEach(job::recordDuplicate);
                        batch.clear();
                    }
                } catch (IllegalArgumentException e) {
                    log.debug("Row {} ERROR: {}", row.rowNumber(), e.getMessage());
                    job.recordInvalid(new TransactionError(
                            row.rowNumber(), row.rawData(), e.getMessage()));
                }
            }

            if (!batch.isEmpty()) {
                transactionRepository.saveAll(batch)
                        .forEach(job::recordDuplicate);
            }

            job.complete();
            log.info("Processing done — jobId={}, valid={}, invalid={}",
                    jobId, job.getValidRows(), job.getInvalidRows());

        } catch (Exception e) {
            log.error("Fatal error — jobId={}: {}", jobId, e.getMessage(), e);
            if (job != null) {
                job.fail("Fatal error: " + e.getMessage());
            }
        } finally {
            if (job != null) {
                importJobRepository.save(job);
            }
        }
    }

    private Transaction parseAndValidate(ParsedTransactionRow row, String jobId) {
        if (row.transactionId() == null || row.transactionId().isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        Iban iban               = validator.validateAndParseIban(row.iban());
        LocalDate date          = validator.validateAndParseDate(row.transactionDate());
        Money money             = validator.validateAndParseMoney(row.amount(), row.currency());
        TransactionCategory cat = validator.parseCategory(row.category());
        return new Transaction(row.transactionId().trim(), jobId, iban, date, money, cat, row.rowNumber());
    }
}
