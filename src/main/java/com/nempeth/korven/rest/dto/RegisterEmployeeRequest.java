package com.nempeth.korven.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterEmployeeRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,
        
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
        String name,
        
        @Size(max = 100, message = "El apellido no puede tener más de 100 caracteres")
        String lastName,
        
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,
        
        @NotBlank(message = "El código del negocio es obligatorio")
        @Size(min = 8, max = 8, message = "El código del negocio debe tener exactamente 8 caracteres")
        String businessJoinCode
) {}