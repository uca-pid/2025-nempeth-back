package com.nempeth.korven.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateSaleItemRequest(
        @NotNull(message = "El producto es obligatorio")
        UUID productId,
        
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        Integer quantity
) {
}