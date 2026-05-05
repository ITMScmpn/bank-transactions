package com.banktransactions.features.importing.domain.model;

/** Błąd walidacji pojedynczego wiersza CSV — wiersz jest odrzucany i nie jest importowany. */
public record TransactionError(int rowNumber, String rawData, String errorMessage) {}
