package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.ForgotPasswordRequest;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterEmployeeRequest;
import com.nempeth.korven.rest.dto.RegisterOwnerRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.rest.dto.RegistrationResponse;
import com.nempeth.korven.rest.dto.ResetPasswordRequest;
import com.nempeth.korven.service.AuthService;
import com.nempeth.korven.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
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

    @PostMapping("/register/owner")
    public ResponseEntity<RegistrationResponse> registerOwner(@Valid @RequestBody RegisterOwnerRequest request) {
        RegistrationResponse response = authService.registerOwner(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register/employee")
    public ResponseEntity<RegistrationResponse> registerEmployee(@Valid @RequestBody RegisterEmployeeRequest request) {
        RegistrationResponse response = authService.registerEmployee(request);
        return ResponseEntity.ok(response);
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
        log.info("Received forgot password request for email: {}", req.email());
        log.info("Request Origin: {}", http.getHeader("Origin"));
        passwordResetService.startReset(req.email(), http);
        log.info("Password reset process initiated successfully");
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
