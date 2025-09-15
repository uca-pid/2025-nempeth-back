package com.nempeth.korven.rest.dto;

public record UpdateUserPasswordRequest(
    String currentPassword,
    String newPassword
) {}
