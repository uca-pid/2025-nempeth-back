package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record BusinessDetailResponse(
        UUID id,
        String name,
        String joinCode,
        Boolean joinCodeEnabled,
        List<BusinessMemberDetailResponse> members,
        List<CategoryResponse> categories,
        List<ProductResponse> products,
        BusinessStatsResponse stats
) {
}