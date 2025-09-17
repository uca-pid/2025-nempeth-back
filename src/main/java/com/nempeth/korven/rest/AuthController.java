package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.ForgotPasswordRequest;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.rest.dto.ResetPasswordRequest;
import com.nempeth.korven.service.AuthService;
import com.nempeth.korven.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

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

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                               HttpServletRequest http) {
        passwordResetService.startReset(req.email(), http);
        // Siempre 200 para no revelar si el mail existe
        return ResponseEntity.ok().build();
    }

    @GetMapping("/password/validate")
    public ResponseEntity<Void> validate(@RequestParam("token") String token) {
        return passwordResetService.validateToken(token)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.GONE).build(); // 410 Gone si expirado/no válido
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

}
