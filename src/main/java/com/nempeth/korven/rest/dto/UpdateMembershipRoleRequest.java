package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipRoleRequest(
        @NotNull(message = "El role de membresía es obligatorio")
        MembershipRole role
) {
}