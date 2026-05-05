package com.banktransactions.infrastructure.exception;

/**
 * Rzucany gdy przesłany plik ma nieobsługiwane rozszerzenie (nie .csv).
 * Mapowany na HTTP 415 Unsupported Media Type.
 */
public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String filename) {
        super("Unsupported file type: '" + filename + "'. Only .csv files are accepted.");
    }
}

