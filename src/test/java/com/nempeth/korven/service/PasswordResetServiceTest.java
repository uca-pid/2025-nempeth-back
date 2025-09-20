package com.nempeth.korven.service;

import com.nempeth.korven.config.AppProperties;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.PasswordResetToken;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.PasswordResetTokenRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.utils.PasswordUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AppProperties appProps;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_TOKEN = "test-token-123";
    private final String FRONTEND_URL = "http://localhost:3000";
    private final int TOKEN_TTL_MINUTES = 30;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .name("John")
                .lastName("Doe")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        testToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(TEST_TOKEN)
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .createdAt(OffsetDateTime.now())
                .usedAt(null)
                .build();
    }

    @Test
    void startReset_withValidEmail_shouldCreateTokenAndSendEmail() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(appProps.getResetTokenTtlMinutes()).thenReturn(TOKEN_TTL_MINUTES);
        when(appProps.getFrontendBaseUrl()).thenReturn(FRONTEND_URL);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        passwordResetService.startReset(TEST_EMAIL, httpServletRequest);

        verify(userRepository).findByEmailIgnoreCase(TEST_EMAIL);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(TEST_EMAIL), anyString());
        verify(appProps).getResetTokenTtlMinutes();
        verify(appProps).getFrontendBaseUrl();
    }

    @Test
    void startReset_withNonExistentEmail_shouldReturnQuietly() {
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        passwordResetService.startReset("nonexistent@example.com", httpServletRequest);

        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void startReset_shouldGenerateUniqueToken() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(appProps.getResetTokenTtlMinutes()).thenReturn(TOKEN_TTL_MINUTES);
        when(appProps.getFrontendBaseUrl()).thenReturn(FRONTEND_URL);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);
        passwordResetService.startReset(TEST_EMAIL, httpServletRequest);

        verify(tokenRepository).save(argThat(token -> 
            token.getToken() != null && 
            token.getToken().length() > 0 &&
            token.getUser().equals(testUser) &&
            token.getExpiresAt() != null &&
            token.getCreatedAt() != null
        ));
    }

    @Test
    void startReset_shouldStripTrailingSlashFromFrontendUrl() {
        String urlWithSlash = FRONTEND_URL + "/";
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(appProps.getResetTokenTtlMinutes()).thenReturn(TOKEN_TTL_MINUTES);
        when(appProps.getFrontendBaseUrl()).thenReturn(urlWithSlash);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        passwordResetService.startReset(TEST_EMAIL, httpServletRequest);
        verify(emailService).sendPasswordResetEmail(eq(TEST_EMAIL), argThat(link -> 
            link.startsWith(FRONTEND_URL + "/reset-password?token=") && 
            !link.contains("//reset-password")
        ));
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        PasswordResetToken validToken = PasswordResetToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .usedAt(null)
                .build();
        when(tokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(validToken));

        boolean result = passwordResetService.validateToken(TEST_TOKEN);

        assertTrue(result);
        verify(tokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    void validateToken_withNonExistentToken_shouldReturnFalse() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        boolean result = passwordResetService.validateToken("invalid-token");

        assertFalse(result);
        verify(tokenRepository).findByToken("invalid-token");
    }

    @Test
    void validateToken_withExpiredToken_shouldReturnFalse() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(OffsetDateTime.now().minusMinutes(10))
                .usedAt(null)
                .build();
        when(tokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(expiredToken));

        boolean result = passwordResetService.validateToken(TEST_TOKEN);

        assertFalse(result);
        verify(tokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    void validateToken_withUsedToken_shouldReturnFalse() {
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .usedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        when(tokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(usedToken));

        boolean result = passwordResetService.validateToken(TEST_TOKEN);

        assertFalse(result);
        verify(tokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    void resetPassword_withValidToken_shouldUpdatePasswordAndMarkTokenUsed() {
        String newPassword = "newPassword123";
        String hashedPassword = "hashedNewPassword";
        
        PasswordResetToken validToken = PasswordResetToken.builder()
                .token(TEST_TOKEN)
                .user(testUser)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .usedAt(null)
                .build();
        
        when(tokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(validToken));
        
        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.hash(newPassword)).thenReturn(hashedPassword);
            passwordResetService.resetPassword(TEST_TOKEN, newPassword);
            verify(tokenRepository).findByToken(TEST_TOKEN);
            assertEquals(hashedPassword, testUser.getPasswordHash());
            assertNotNull(validToken.getUsedAt());
            passwordUtilsMock.verify(() -> PasswordUtils.hash(newPassword));
        }
    }

    @Test
    void resetPassword_withInvalidToken_shouldThrowException() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> passwordResetService.resetPassword("invalid-token", "newPassword"));
        
        assertEquals("Token inválido", exception.getMessage());
        verify(tokenRepository).findByToken("invalid-token");
    }

    @Test
    void resetPassword_withExpiredToken_shouldThrowException() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token(TEST_TOKEN)
                .user(testUser)
                .expiresAt(OffsetDateTime.now().minusMinutes(10))
                .usedAt(null)
                .build();
        when(tokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(expiredToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> passwordResetService.resetPassword(TEST_TOKEN, "newPassword"));
        
        assertEquals("Token expirado o ya utilizado", exception.getMessage());
        verify(tokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    void resetPassword_withUsedToken_shouldThrowException() {
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .token(TEST_TOKEN)
                .user(testUser)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .usedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        when(tokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(usedToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> passwordResetService.resetPassword(TEST_TOKEN, "newPassword"));
        
        assertEquals("Token expirado o ya utilizado", exception.getMessage());
        verify(tokenRepository).findByToken(TEST_TOKEN);
    }
}