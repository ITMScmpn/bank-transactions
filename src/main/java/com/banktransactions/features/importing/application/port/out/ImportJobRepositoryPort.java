package com.banktransactions.features.importing.application.port.out;

import com.banktransactions.features.importing.domain.model.ImportJob;

import java.util.List;
import java.util.Optional;

/** Persystencja ImportJob. Implementacja: MongoImportJobAdapter. */
public interface ImportJobRepositoryPort {
    ImportJob save(ImportJob importJob);
    Optional<ImportJob> findById(String id);
    List<ImportJob> findAll();
}
