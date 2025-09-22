package com.nempeth.korven.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationExceptionTest {

    @Test
    void authenticationException_ShouldHaveUnauthorizedStatus() {
        // Given
        String errorMessage = "Invalid credentials";

        // When
        AuthenticationException exception = new AuthenticationException(errorMessage);

        // Then
        assertEquals(errorMessage, exception.getMessage());
        
        // Verify the @ResponseStatus annotation
        ResponseStatus responseStatus = AuthenticationException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.UNAUTHORIZED, responseStatus.value());
    }

    @Test
    void authenticationException_ShouldSupportCause() {
        // Given
        String errorMessage = "Authentication failed";
        Throwable cause = new RuntimeException("Database connection failed");

        // When
        AuthenticationException exception = new AuthenticationException(errorMessage, cause);

        // Then
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}