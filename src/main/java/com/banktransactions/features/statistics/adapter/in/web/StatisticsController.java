package com.banktransactions.features.statistics.adapter.in.web;

import com.banktransactions.features.statistics.adapter.in.web.dto.StatisticEntryResponse;
import com.banktransactions.features.statistics.application.port.in.GetStatisticsUseCase;
import com.banktransactions.features.statistics.domain.model.GroupBy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "Statistics", description = "Transaction statistics aggregated by category, IBAN or month")
public class StatisticsController {

    private final GetStatisticsUseCase statisticsUseCase;

    public StatisticsController(GetStatisticsUseCase statisticsUseCase) {
        this.statisticsUseCase = statisticsUseCase;
    }

    /**
     * GET /api/v1/statistics?groupBy=CATEGORY|IBAN|MONTH
     *   &fromYear=2024&fromMonth=1&toYear=2024&toMonth=12
     */
    @Operation(
        summary = "Get aggregated transaction statistics",
        description = "Returns transaction statistics grouped by the selected field. "
                    + "Optionally filtered by a date range (inclusive). "
                    + "Provide fromYear+fromMonth and/or toYear+toMonth to limit results."
    )
    @ApiResponse(responseCode = "200", description = "Statistics returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid groupBy value or date parameters")
    @GetMapping
    public List<StatisticEntryResponse> getStatistics(
            @Parameter(description = "Field to group results by", example = "CATEGORY")
            @RequestParam(defaultValue = "CATEGORY") GroupBy groupBy,
            @Parameter(description = "Start year (inclusive)", example = "2024")
            @Nullable @RequestParam(required = false) Integer fromYear,
            @Parameter(description = "Start month 1–12 (inclusive)", example = "1")
            @Nullable @RequestParam(required = false) Integer fromMonth,
            @Parameter(description = "End year (inclusive)", example = "2024")
            @Nullable @RequestParam(required = false) Integer toYear,
            @Parameter(description = "End month 1–12 (inclusive)", example = "12")
            @Nullable @RequestParam(required = false) Integer toMonth) {

        YearMonth from = (fromYear != null && fromMonth != null) ? YearMonth.of(fromYear, fromMonth) : null;
        YearMonth to   = (toYear   != null && toMonth   != null) ? YearMonth.of(toYear, toMonth)     : null;

        return statisticsUseCase.getStatistics(groupBy, from, to)
                .stream()
                .map(StatisticEntryResponse::from)
                .toList();
    }
}
