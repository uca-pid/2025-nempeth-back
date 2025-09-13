package com.nempeth.korven.service;

import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional
    public UUID register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email ya registrado");
        }
        if (req.role() == null) {
            throw new IllegalArgumentException("Role obligatorio (USER u OWNER)");
        }
        String hash = PasswordUtils.hash(req.password());
        User user = User.builder()
                .email(req.email())
                .name(req.name())
                .lastName(req.lastName())
                .passwordHash(hash)
                .role(req.role())
                .build();

        userRepository.save(user);
        return user.getId();
    }

    @Transactional(readOnly = true)
    public UUID login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (!PasswordUtils.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        return user.getId();
    }

    @Transactional(readOnly = true)
    public Role getRole(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getRole)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}
