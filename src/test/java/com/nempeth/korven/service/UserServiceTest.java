package com.nempeth.korven.service;

import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.UpdateUserPasswordRequest;
import com.nempeth.korven.rest.dto.UpdateUserProfileRequest;
import com.nempeth.korven.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;
    private String userEmail;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEmail = "test@example.com";
        hashedPassword = "hashedPassword123";

        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .name("John")
                .lastName("Doe")
                .passwordHash(hashedPassword)
                .role(Role.USER)
                .build();
    }

    // updateUserProfile tests
    @Test
    void updateUserProfile_withValidDataAndSameRequester_shouldUpdateSuccessfully() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("jane@example.com", "Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean emailChanged = userService.updateUserProfile(userId, userEmail, request);

        // Then
        assertTrue(emailChanged);
        assertEquals("Jane", testUser.getName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals("jane@example.com", testUser.getEmail());
        verify(userRepository).findById(userId);
        verify(userRepository).findByEmailIgnoreCase("jane@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_withSameEmail_shouldUpdateWithoutEmailChange() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(userEmail, "Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean emailChanged = userService.updateUserProfile(userId, userEmail, request);

        // Then
        assertFalse(emailChanged);
        assertEquals("Jane", testUser.getName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals(userEmail, testUser.getEmail()); // Should remain unchanged
        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_withNullFields_shouldUpdateOnlyNonNullFields() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, "Jane", null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean emailChanged = userService.updateUserProfile(userId, userEmail, request);

        // Then
        assertFalse(emailChanged);
        assertEquals("Jane", testUser.getName());
        assertEquals("Doe", testUser.getLastName()); // Should remain unchanged
        assertEquals(userEmail, testUser.getEmail()); // Should remain unchanged
        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_withBlankEmail_shouldNotUpdateEmail() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("   ", "Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean emailChanged = userService.updateUserProfile(userId, userEmail, request);

        // Then
        assertFalse(emailChanged);
        assertEquals("Jane", testUser.getName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals(userEmail, testUser.getEmail()); // Should remain unchanged
        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_withInvalidEmailFormat_shouldThrowException() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("invalid-email", "Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserProfile(userId, userEmail, request));
        assertEquals("Email con formato inválido", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_withExistingEmail_shouldThrowException() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("existing@example.com", "Jane", "Smith");
        User existingUser = User.builder().id(UUID.randomUUID()).email("existing@example.com").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(existingUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserProfile(userId, userEmail, request));
        assertEquals("Email ya registrado", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository).findByEmailIgnoreCase("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_withSameUserButDifferentCase_shouldAllowUpdate() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("EXISTING@EXAMPLE.COM", "Jane", "Smith");
        User existingUser = User.builder().id(userId).email("existing@example.com").build(); // Same user ID
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmailIgnoreCase("EXISTING@EXAMPLE.COM")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean emailChanged = userService.updateUserProfile(userId, userEmail, request);

        // Then
        assertTrue(emailChanged);
        assertEquals("Jane", testUser.getName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals("EXISTING@EXAMPLE.COM", testUser.getEmail());
        verify(userRepository).findById(userId);
        verify(userRepository).findByEmailIgnoreCase("EXISTING@EXAMPLE.COM");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_withNonExistentUser_shouldThrowException() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("jane@example.com", "Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserProfile(userId, userEmail, request));
        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_withDifferentRequester_shouldThrowAccessDeniedException() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("jane@example.com", "Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.updateUserProfile(userId, "different@example.com", request));
        assertEquals("No autorizado para modificar este usuario", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // updateUserPassword tests
    @Test
    void updateUserPassword_withValidData_shouldUpdatePassword() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("currentPassword", "newPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches("currentPassword", hashedPassword)).thenReturn(true);
            passwordUtilsMock.when(() -> PasswordUtils.hash("newPassword")).thenReturn("newHashedPassword");

            // When
            userService.updateUserPassword(userId, userEmail, request);

            // Then
            assertEquals("newHashedPassword", testUser.getPasswordHash());
            verify(userRepository).save(testUser);
            passwordUtilsMock.verify(() -> PasswordUtils.matches("currentPassword", hashedPassword));
            passwordUtilsMock.verify(() -> PasswordUtils.hash("newPassword"));
        }
    }

    @Test
    void updateUserPassword_withNonExistentUser_shouldThrowException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("currentPassword", "newPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserPassword(userId, userEmail, request));
        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_withDifferentRequester_shouldThrowAccessDeniedException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("currentPassword", "newPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.updateUserPassword(userId, "different@example.com", request));
        assertEquals("No autorizado para modificar este usuario", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_withNullCurrentPassword_shouldThrowException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(null, "newPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserPassword(userId, userEmail, request));
        assertEquals("La contraseña actual es requerida", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_withBlankCurrentPassword_shouldThrowException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("   ", "newPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserPassword(userId, userEmail, request));
        assertEquals("La contraseña actual es requerida", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_withIncorrectCurrentPassword_shouldThrowException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("wrongPassword", "newPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches("wrongPassword", hashedPassword)).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUserPassword(userId, userEmail, request));
            assertEquals("La contraseña actual es incorrecta", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    void updateUserPassword_withNullNewPassword_shouldThrowException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("currentPassword", null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches("currentPassword", hashedPassword)).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUserPassword(userId, userEmail, request));
            assertEquals("La nueva contraseña no puede estar vacía", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    void updateUserPassword_withBlankNewPassword_shouldThrowException() {
        // Given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("currentPassword", "   ");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
            passwordUtilsMock.when(() -> PasswordUtils.matches("currentPassword", hashedPassword)).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUserPassword(userId, userEmail, request));
            assertEquals("La nueva contraseña no puede estar vacía", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    // deleteUser tests
    @Test
    void deleteUser_withValidData_shouldDeleteUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(userId, userEmail);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_withNonExistentUser_shouldThrowException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteUser(userId, userEmail));
        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_withDifferentRequester_shouldThrowAccessDeniedException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.deleteUser(userId, "different@example.com"));
        assertEquals("No autorizado para borrar este usuario", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    // getUserByEmail tests
    @Test
    void getUserByEmail_withExistingEmail_shouldReturnUser() {
        // Given
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByEmail(userEmail);

        // Then
        assertEquals(testUser, result);
        verify(userRepository).findByEmailIgnoreCase(userEmail);
    }

    @Test
    void getUserByEmail_withNonExistentEmail_shouldThrowException() {
        // Given
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserByEmail("nonexistent@example.com"));
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void getUserByEmail_withCaseInsensitiveEmail_shouldReturnUser() {
        // Given
        when(userRepository.findByEmailIgnoreCase("TEST@EXAMPLE.COM")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByEmail("TEST@EXAMPLE.COM");

        // Then
        assertEquals(testUser, result);
        verify(userRepository).findByEmailIgnoreCase("TEST@EXAMPLE.COM");
    }
}