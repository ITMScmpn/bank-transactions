package com.banktransactions.features.importing.adapter.in.web;

import com.banktransactions.features.importing.adapter.in.web.dto.TransactionResponse;
import com.banktransactions.features.importing.application.port.in.ListTransactionsUseCase;
import com.banktransactions.features.importing.domain.model.TransactionCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Browse imported transactions")
public class TransactionController {

    private final ListTransactionsUseCase listTransactionsUseCase;

    public TransactionController(ListTransactionsUseCase listTransactionsUseCase) {
        this.listTransactionsUseCase = listTransactionsUseCase;
    }

    /**
     * GET /api/v1/transactions
     * Zwraca transakcje z opcjonalnym filtrowaniem po IBAN, kategorii, roku i/lub miesiącu.
     */
    @Operation(summary = "List transactions", description = "All parameters are optional and can be combined.")
    @ApiResponse(responseCode = "200", description = "List of transactions")
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(
            @Parameter(description = "Filter by IBAN") @RequestParam(required = false) String iban,
            @Parameter(description = "Filter by category") @RequestParam(required = false) TransactionCategory category,
            @Parameter(description = "Filter by year (e.g. 2024)") @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by month (1-12)") @RequestParam(required = false) Integer month) {

        List<TransactionResponse> result = listTransactionsUseCase
                .findFiltered(iban, category, year, month)
                .stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}

