package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.CategoryType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        String displayName,
        String icon
) {
}