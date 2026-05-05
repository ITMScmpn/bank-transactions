package com.banktransactions.features.importing.application.service;

import com.banktransactions.features.importing.application.port.in.ListImportsUseCase;
import com.banktransactions.features.importing.application.port.out.ImportJobRepositoryPort;
import com.banktransactions.features.importing.domain.model.ImportJob;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListImportsService implements ListImportsUseCase {

    private final ImportJobRepositoryPort importJobRepository;

    public ListImportsService(ImportJobRepositoryPort importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @Override
    public List<ImportJob> listAll() {
        return importJobRepository.findAll();
    }
}

