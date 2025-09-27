package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RegistrationResponse(
        UUID userId,
        String message,
        BusinessResponse business
) {}