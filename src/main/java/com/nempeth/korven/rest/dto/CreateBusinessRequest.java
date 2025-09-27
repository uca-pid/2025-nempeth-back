package com.nempeth.korven.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBusinessRequest(
        @NotBlank(message = "El nombre del negocio es obligatorio")
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
        String name
) {
}