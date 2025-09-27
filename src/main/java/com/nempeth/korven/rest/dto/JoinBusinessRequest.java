package com.nempeth.korven.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinBusinessRequest(
        @NotBlank(message = "El código de acceso es obligatorio")
        @Size(min = 6, max = 12, message = "El código debe tener entre 6 y 12 caracteres")
        String joinCode
) {
}