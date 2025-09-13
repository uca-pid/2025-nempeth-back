package com.nempeth.korven.rest.dto;

public record LoginRequest(
        String email,
        String password
) {}
