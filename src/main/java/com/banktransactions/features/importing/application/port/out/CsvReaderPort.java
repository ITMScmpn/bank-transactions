package com.banktransactions.features.importing.application.port.out;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Odczyt pliku CSV jako lista surowych wierszy.
 * Domena nie zna formatu CSV — zna tylko ParsedTransactionRow.
 * Implementacja: CommonsCsvReaderAdapter.
 */
public interface CsvReaderPort {

    List<ParsedTransactionRow> read(InputStream inputStream) throws IOException;

    /** Surowy wiersz CSV po sparsowaniu — jeszcze niezwalidowany domenowo. */
    record ParsedTransactionRow(
            int rowNumber,
            String transactionId,
            String iban,
            String transactionDate,
            String currency,
            String category,
            String amount,
            String rawData
    ) {}
}
