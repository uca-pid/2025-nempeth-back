package com.nempeth.korven.rest;

import com.nempeth.korven.config.TestMailConfiguration;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.rest.dto.UpdateUserPasswordRequest;
import com.nempeth.korven.rest.dto.UpdateUserProfileRequest;
import com.nempeth.korven.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestMailConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void getCurrentUser_shouldReturnUserData() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest("user@test.com", "John", "Doe", "password123", Role.USER);
        UUID userId = authService.register(registerRequest);
        LoginRequest loginRequest = new LoginRequest("user@test.com", "password123");
        String token = authService.loginAndIssueToken(loginRequest);

        // Act & Assert
        mockMvc.perform(get("/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getCurrentUser_withoutToken_shouldReturn403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserProfile_shouldUpdateProfileFields() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("profile@test.com", "Ana", "Perez", "password123", Role.USER);
        UUID userId = authService.register(registerRequest);
        LoginRequest loginRequest = new LoginRequest("profile@test.com", "password123");
        String token = authService.loginAndIssueToken(loginRequest);

        UpdateUserProfileRequest updateReq = new UpdateUserProfileRequest("profile2@test.com", "Anita", "Pereira");
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/users/" + userId + "/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailChanged").value(true));
    }

    @Test
    void updateUserPassword_shouldUpdatePassword() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("pass@test.com", "Pablo", "Gomez", "password123", Role.USER);
        UUID userId = authService.register(registerRequest);
        LoginRequest loginRequest = new LoginRequest("pass@test.com", "password123");
        String token = authService.loginAndIssueToken(loginRequest);

        UpdateUserPasswordRequest updateReq = new UpdateUserPasswordRequest("password123", "newpass456");
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/users/" + userId + "/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada"));
    }
}