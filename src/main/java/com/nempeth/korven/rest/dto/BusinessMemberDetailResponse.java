package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.MembershipRole;
import com.nempeth.korven.constants.MembershipStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record BusinessMemberDetailResponse(
        UUID userId,
        String userEmail,
        String userName,
        String userLastName,
        MembershipRole role,
        MembershipStatus status
) {
}