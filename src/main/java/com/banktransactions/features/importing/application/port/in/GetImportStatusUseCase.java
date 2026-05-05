package com.banktransactions.features.importing.application.port.in;

import com.banktransactions.features.importing.domain.model.ImportJob;

public interface GetImportStatusUseCase {
    ImportJob getStatus(String importJobId);
}
