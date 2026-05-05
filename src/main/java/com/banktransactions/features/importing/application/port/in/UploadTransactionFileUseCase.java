package com.banktransactions.features.importing.application.port.in;

import org.springframework.web.multipart.MultipartFile;

/** Przyjmuje plik CSV, tworzy ImportJob i inicjuje asynchroniczne przetwarzanie. */
public interface UploadTransactionFileUseCase {
    /** @return ID utworzonego ImportJob */
    String upload(MultipartFile file);
}
