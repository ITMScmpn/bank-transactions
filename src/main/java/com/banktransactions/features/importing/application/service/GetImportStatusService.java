package com.banktransactions.features.importing.application.service;

import com.banktransactions.features.importing.application.port.in.GetImportStatusUseCase;
import com.banktransactions.features.importing.application.port.out.ImportJobRepositoryPort;
import com.banktransactions.features.importing.domain.model.ImportJob;
import com.banktransactions.features.importing.domain.exception.ImportJobNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetImportStatusService implements GetImportStatusUseCase {

    private final ImportJobRepositoryPort importJobRepository;

    public GetImportStatusService(ImportJobRepositoryPort importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @Override
    public ImportJob getStatus(String importJobId) {
        return importJobRepository.findById(importJobId)
                .orElseThrow(() -> new ImportJobNotFoundException(importJobId));
    }
}
