package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.Role;

public record UserResponse(
        String email,
        String name,
        String lastName,
        Role role
) {}