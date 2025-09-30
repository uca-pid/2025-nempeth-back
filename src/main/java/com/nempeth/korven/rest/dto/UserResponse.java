package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.MembershipRole;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String email,
        String name,
        String lastName,
        List<BusinessMembershipResponse> businesses
) {
}