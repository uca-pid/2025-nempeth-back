package com.nempeth.korven.rest.dto;

public record UpdateUserRequest(
        String email,
        String name,
        String lastName,
        String newPassword
) {}
