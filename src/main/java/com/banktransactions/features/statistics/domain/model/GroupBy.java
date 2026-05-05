package com.banktransactions.features.statistics.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Field to group statistics by")
public enum GroupBy {
    CATEGORY,
    IBAN,
    MONTH
}

