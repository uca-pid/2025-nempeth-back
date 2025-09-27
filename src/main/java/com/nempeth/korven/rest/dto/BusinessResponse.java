package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BusinessResponse(
        UUID id,
        String name,
        String joinCode,
        Boolean joinCodeEnabled
) {
}