package com.nempeth.korven.rest;

import com.nempeth.korven.constants.Role;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        UUID id = authService.register(req);
        return ResponseEntity.ok(Map.of("userId", id.toString()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String token = authService.loginAndIssueToken(req);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "message", "Login exitoso"
        ));
    }

}
