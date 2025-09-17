package com.nempeth.korven.service;

import com.nempeth.korven.config.AppProperties;
import com.nempeth.korven.persistence.entity.PasswordResetToken;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.PasswordResetTokenRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.utils.PasswordUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final AppProperties appProps;

    private static final SecureRandom RNG = new SecureRandom();

    @Transactional
    public void startReset(String email, HttpServletRequest request) {
        Optional<User> optUser = userRepository.findByEmailIgnoreCase(email);
        if (optUser.isEmpty()) {
            // No filtramos existencia: respondemos 200 igual por seguridad
            return;
        }
        User user = optUser.get();

        // Generar token seguro (32 bytes -> 43 chars Base64 URL-safe)
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken prt = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusMinutes(appProps.getResetTokenTtlMinutes()))
                .createdAt(OffsetDateTime.now())
                .build();

        tokenRepository.save(prt);

        // Link al frontend: /reset-password?token=XYZ
        String resetLink = appProps.getFrontendBaseUrl().replaceAll("/+$","")
                + "/reset-password?token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired() && !t.isUsed())
                .isPresent();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (prt.isExpired() || prt.isUsed()) {
            throw new IllegalArgumentException("Token expirado o ya utilizado");
        }

        User user = prt.getUser();
        user.setPasswordHash(PasswordUtils.hash(newPassword));
        // marcar token como usado
        prt.setUsedAt(OffsetDateTime.now());

        // JPA hace flush al terminar la transacción
    }

    private String getClientIp(HttpServletRequest request) {
        String h = request.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
