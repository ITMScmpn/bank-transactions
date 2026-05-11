package com.banktransactions.features.importing.application.service;

import com.banktransactions.features.importing.application.port.in.UploadTransactionFileUseCase;
import com.banktransactions.features.importing.application.port.out.AsyncImportTriggerPort;
import com.banktransactions.features.importing.application.port.out.ImportJobRepositoryPort;
import com.banktransactions.features.importing.domain.model.ImportJob;
import com.banktransactions.features.importing.domain.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestruje upload:
 * 1. Waliduje plik (rozszerzenie, rozmiar)
 * 2. Tworzy ImportJob (PENDING)
 * 3. Przekazuje do AsyncImportTriggerPort (@Async)
 *
 * Nazwy plików są losowe i nieunikatowe — każdy upload tworzy niezależny ImportJob.
 * Dedulikacja transakcji odbywa się na poziomie transactionId z CSV.
 */
@Service
public class UploadTransactionFileService implements UploadTransactionFileUseCase {

    private final ImportJobRepositoryPort importJobRepository;
    private final AsyncImportTriggerPort asyncImportTrigger;

    public UploadTransactionFileService(ImportJobRepositoryPort importJobRepository,
                                        AsyncImportTriggerPort asyncImportTrigger) {
        this.importJobRepository = importJobRepository;
        this.asyncImportTrigger = asyncImportTrigger;
    }

    @Override
    public String upload(MultipartFile file) {
        validateFile(file);

        try {
            String jobId = UUID.randomUUID().toString();
            ImportJob job = new ImportJob(jobId, file.getOriginalFilename(), LocalDateTime.now());
            importJobRepository.save(job);

            byte[] fileBytes = file.getBytes();
            asyncImportTrigger.trigger(jobId, new ByteArrayInputStream(fileBytes));
            return jobId;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new UnsupportedFileTypeException(filename != null ? filename : "(unknown)");
        }
    }
}
