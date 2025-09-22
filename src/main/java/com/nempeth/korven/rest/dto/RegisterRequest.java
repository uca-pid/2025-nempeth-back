package com.nempeth.korven.rest.dto;

import com.nempeth.korven.constants.Role;

public record RegisterRequest(
        String email,
        String name,
        String lastName,
        String password,
        Role role
) {}
