package com.banktransactions.features.importing.application.port.in;

import com.banktransactions.features.importing.domain.model.ImportJob;

import java.util.List;

public interface ListImportsUseCase {
    List<ImportJob> listAll();
}

