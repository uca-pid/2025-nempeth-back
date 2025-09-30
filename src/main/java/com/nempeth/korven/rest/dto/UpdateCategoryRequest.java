package com.nempeth.korven.rest.dto;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
        String name,
        
        @Size(max = 100, message = "El nombre para mostrar no puede tener más de 100 caracteres")
        String displayName,
        
        @Size(max = 50, message = "El ícono no puede tener más de 50 caracteres")
        String icon
) {
}