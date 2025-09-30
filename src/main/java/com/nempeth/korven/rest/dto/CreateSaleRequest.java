package com.nempeth.korven.rest.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSaleRequest(
        @NotEmpty(message = "La venta debe tener al menos un item")
        List<CreateSaleItemRequest> items
) {
}