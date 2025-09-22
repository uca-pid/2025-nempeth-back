package com.nempeth.korven.service;

import com.nempeth.korven.constants.Role;
import com.nempeth.korven.exception.AuthenticationException;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.utils.JwtUtils;
import com.nempeth.korven.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String HASHED_PASSWORD = "hashedPassword123";
    private final String JWT_TOKEN = "jwt.token.here";
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email(TEST_EMAIL)
                .name("John")
                .lastName("Doe")
                .passwordHash(HASHED_PASSWORD)
                .role(Role.USER)
                .build();

        validRegisterRequest = new RegisterRequest(
                TEST_EMAIL,
                "John",
                "Doe",
                TEST_PASSWORD,
                Role.USER
        );

        validLoginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
    }

    @Test
    void register_withValidRequest_shouldCreateUserAndReturnId() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.hash(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

            UUID result = authService.register(validRegisterRequest);

            assertEquals(USER_ID, result);
            verify(userRepository).existsByEmail(TEST_EMAIL);
            verify(userRepository).save(argThat(user -> 
                user.getEmail().equals(TEST_EMAIL) &&
                user.getName().equals("John") &&
                user.getLastName().equals("Doe") &&
                user.getPasswordHash().equals(HASHED_PASSWORD) &&
                user.getRole().equals(Role.USER)
            ));
            passwordUtilsMock.verify(() -> PasswordUtils.hash(TEST_PASSWORD));
        }
    }

    @Test
    void register_withExistingEmail_shouldThrowException() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.register(validRegisterRequest));

        assertEquals("Email ya registrado", exception.getMessage());
        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withNullRole_shouldThrowException() {
        RegisterRequest requestWithNullRole = new RegisterRequest(
                TEST_EMAIL,
                "John",
                "Doe",
                TEST_PASSWORD,
                null
        );
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.register(requestWithNullRole));

        assertEquals("Role obligatorio (USER u OWNER)", exception.getMessage());
        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withOwnerRole_shouldCreateOwnerUser() {
        RegisterRequest ownerRequest = new RegisterRequest(
                "owner@example.com",
                "Owner",
                "User",
                TEST_PASSWORD,
                Role.OWNER
        );

        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.hash(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

            UUID result = authService.register(ownerRequest);
            assertNotNull(result);
            verify(userRepository).save(argThat(user -> user.getRole().equals(Role.OWNER)));
        }
    }

    @Test
    void loginAndIssueToken_withValidCredentials_shouldReturnToken() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(eq(TEST_EMAIL), anyMap())).thenReturn(JWT_TOKEN);

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            String result = authService.loginAndIssueToken(validLoginRequest);
            assertEquals(JWT_TOKEN, result);
            verify(userRepository).findByEmailIgnoreCase(TEST_EMAIL);
            verify(jwtUtils).generateToken(eq(TEST_EMAIL), argThat(claims -> 
                claims.get("userId").equals(USER_ID.toString()) &&
                claims.get("role").equals(Role.USER.name())
            ));
            passwordUtilsMock.verify(() -> PasswordUtils.matches(TEST_PASSWORD, HASHED_PASSWORD));
        }
    }

    @Test
    void loginAndIssueToken_withNonExistentUser_shouldThrowAuthenticationException() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());

        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authService.loginAndIssueToken(validLoginRequest));

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(userRepository).findByEmailIgnoreCase(TEST_EMAIL);
        verify(jwtUtils, never()).generateToken(anyString(), any());
    }

    @Test
    void loginAndIssueToken_withWrongPassword_shouldThrowAuthenticationException() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authService.loginAndIssueToken(validLoginRequest));

            assertEquals("Credenciales inválidas", exception.getMessage());
            verify(userRepository).findByEmailIgnoreCase(TEST_EMAIL);
            verify(jwtUtils, never()).generateToken(anyString(), any());
            passwordUtilsMock.verify(() -> PasswordUtils.matches(TEST_PASSWORD, HASHED_PASSWORD));
        }
    }

    @Test
    void loginAndIssueToken_caseInsensitiveEmail_shouldWork() {
        String emailWithDifferentCase = "TEST@EXAMPLE.COM";
        LoginRequest requestWithDifferentCase = new LoginRequest(emailWithDifferentCase, TEST_PASSWORD);
        
        when(userRepository.findByEmailIgnoreCase(emailWithDifferentCase)).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(eq(TEST_EMAIL), anyMap())).thenReturn(JWT_TOKEN);

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            String result = authService.loginAndIssueToken(requestWithDifferentCase);
            assertEquals(JWT_TOKEN, result);
            verify(userRepository).findByEmailIgnoreCase(emailWithDifferentCase);
            verify(jwtUtils).generateToken(eq(TEST_EMAIL), anyMap());
        }
    }

    @Test
    void getRole_withValidUserId_shouldReturnRole() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        Role result = authService.getRole(USER_ID);
        assertEquals(Role.USER, result);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void getRole_withNonExistentUserId_shouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.getRole(nonExistentId));

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void getRole_withOwnerUser_shouldReturnOwnerRole() {
        User ownerUser = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .name("Owner")
                .lastName("User")
                .passwordHash(HASHED_PASSWORD)
                .role(Role.OWNER)
                .build();
        when(userRepository.findById(ownerUser.getId())).thenReturn(Optional.of(ownerUser));
        Role result = authService.getRole(ownerUser.getId());
        assertEquals(Role.OWNER, result);
        verify(userRepository).findById(ownerUser.getId());
    }

    @Test
    void register_withEmptyStrings_shouldStillCreateUser() {
        RegisterRequest emptyFieldsRequest = new RegisterRequest(
                "",
                "",
                "",
                "",
                Role.USER
        );

        when(userRepository.existsByEmail("")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.hash("")).thenReturn("hashedEmptyPassword");
            UUID result = authService.register(emptyFieldsRequest);
            assertNotNull(result);
            verify(userRepository).existsByEmail("");
            verify(userRepository).save(argThat(user -> 
                user.getEmail().equals("") &&
                user.getName().equals("") &&
                user.getLastName().equals("") &&
                user.getRole().equals(Role.USER)
            ));
        }
    }

    @Test
    void loginAndIssueToken_shouldIncludeCorrectClaimsInToken() {
        User ownerUser = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .passwordHash(HASHED_PASSWORD)
                .role(Role.OWNER)
                .build();
        LoginRequest ownerLogin = new LoginRequest("owner@example.com", TEST_PASSWORD);
        
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(jwtUtils.generateToken(eq("owner@example.com"), anyMap())).thenReturn(JWT_TOKEN);

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            String result = authService.loginAndIssueToken(ownerLogin);

            assertEquals(JWT_TOKEN, result);
            verify(jwtUtils).generateToken(eq("owner@example.com"), argThat(claims -> {
                String userId = (String) claims.get("userId");
                String role = (String) claims.get("role");
                return userId.equals(ownerUser.getId().toString()) && role.equals("OWNER");
            }));
        }
    }
}