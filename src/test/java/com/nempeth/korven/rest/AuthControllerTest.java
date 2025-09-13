package com.nempeth.korven.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void register_owner_shouldCreateUserAndReturnId() throws Exception {
        var req = new RegisterRequest(
                "owner@test.com",
                "Lucas",
                "Heredia",
                "123456",
                Role.OWNER
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", not(emptyString())));
    }

    @Test
    void login_withCorrectCredentials_shouldReturnToken() throws Exception {
        // Pre: registrar usuario
        var reg = new RegisterRequest("login@test.com", "User", "Test", "123456", Role.OWNER);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        var login = new LoginRequest("login@test.com", "123456");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.message", is("Login exitoso")));
    }

    @Test
    void login_withWrongPassword_shouldReturn400() throws Exception {
        var reg = new RegisterRequest("badpass@test.com", "User", "Test", "correct-pass", Role.USER);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        var login = new LoginRequest("badpass@test.com", "wrong-pass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Credenciales inválidas")));
    }

    @Test
    void register_withExistingEmail_shouldReturn400() throws Exception {
        var req = new RegisterRequest("dup@test.com", "Dup", "User", "123456", Role.USER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Email ya registrado")));
    }
}
