package com.banktransactions.features.importing.adapter.out.csv;

import com.banktransactions.features.importing.application.port.out.CsvReaderPort;
import com.banktransactions.features.importing.domain.exception.InvalidCsvStructureException;
import com.banktransactions.features.importing.domain.exception.TooManyRowsException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementuje CsvReaderPort przez Apache Commons CSV.
 * Odpowiada wyłącznie za:
 * 1. Walidację struktury pliku (wymagane nagłówki)
 * 2. Parsowanie rekordów do ParsedTransactionRow
 * 3. Egzekwowanie limitu wierszy (max-rows)
 * Zero logiki domenowej — to jest adapter, nie domena.
 */
@Component
public class CommonsCsvReaderAdapter implements CsvReaderPort {

    private static final Set<String> REQUIRED_HEADERS =
            Set.of("transactionId", "iban", "transactionDate", "currency", "category", "amount");

    // Odczyt nagłówków z samego pliku — nie wymuszamy kolejności kolumn
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    @Value("${app.max-rows:10000}")
    private int maxRows;

    @Override
    public List<ParsedTransactionRow> read(InputStream inputStream) throws IOException {
        List<ParsedTransactionRow> rows = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(inputStream, StandardCharsets.UTF_8, FORMAT)) {

            validateHeaders(parser.getHeaderMap());

            int rowNumber = 1; // wiersz 1 = nagłówek

            for (CSVRecord record : parser) {
                rowNumber++;

                if (rowNumber - 1 > maxRows) {
                    throw new TooManyRowsException(rowNumber - 1, maxRows);
                }

                rows.add(new ParsedTransactionRow(
                        rowNumber,
                        safeGet(record, "transactionId"),
                        safeGet(record, "iban"),
                        safeGet(record, "transactionDate"),
                        safeGet(record, "currency"),
                        safeGet(record, "category"),
                        safeGet(record, "amount"),
                        record.toString()
                ));
            }
        }

        return rows;
    }

    private void validateHeaders(Map<String, Integer> headerMap) {
        if (headerMap == null || headerMap.isEmpty()) {
            throw new InvalidCsvStructureException(REQUIRED_HEADERS);
        }
        Set<String> missing = new HashSet<>(REQUIRED_HEADERS);
        missing.removeAll(headerMap.keySet());
        if (!missing.isEmpty()) {
            throw new InvalidCsvStructureException(missing);
        }
    }

    private String safeGet(CSVRecord record, String header) {
        try { return record.get(header); } catch (Exception e) { return ""; }
    }
}
