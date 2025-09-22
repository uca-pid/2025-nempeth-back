package com.nempeth.korven.rest.dto;

public record UpdateUserProfileRequest(
    String email,
    String name,
    String lastName
) {}
