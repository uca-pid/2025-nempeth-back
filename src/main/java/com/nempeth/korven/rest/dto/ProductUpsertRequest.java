package com.nempeth.korven.rest.dto;

import java.math.BigDecimal;

public record ProductUpsertRequest(
        String name,
        String description,
        BigDecimal price
) {}
