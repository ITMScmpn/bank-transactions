package com.banktransactions.features.importing.domain.exception;

/** Rzucany gdy ImportJob o podanym ID nie istnieje w bazie. Mapowany na HTTP 404. */
public class ImportJobNotFoundException extends RuntimeException {
    public ImportJobNotFoundException(String id) {
        super("Import job not found: " + id);
    }
}

