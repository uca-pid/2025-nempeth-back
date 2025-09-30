package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.MembershipStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipStatusRequest(
        @NotNull(message = "El status de membresía es obligatorio")
        MembershipStatus status
) {
}