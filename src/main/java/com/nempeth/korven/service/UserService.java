package com.nempeth.korven.service;

import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.UpdateUserProfileRequest;
import com.nempeth.korven.rest.dto.UpdateUserPasswordRequest;
import com.nempeth.korven.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private static final Pattern EMAIL_RX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);


    @Transactional
    public boolean updateUserProfile(UUID userId, String requesterEmail, UpdateUserProfileRequest req) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!target.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("No autorizado para modificar este usuario");
        }
        boolean emailChanged = false;

        if (req.email() != null && !req.email().isBlank()
                && !req.email().equalsIgnoreCase(target.getEmail())) {

            if (!EMAIL_RX.matcher(req.email()).matches()) {
                throw new IllegalArgumentException("Email con formato inválido");
            }

            userRepository.findByEmailIgnoreCase(req.email()).ifPresent(existing -> {
                if (!existing.getId().equals(target.getId())) {
                    throw new IllegalArgumentException("Email ya registrado");
                }
            });

            target.setEmail(req.email());
            emailChanged = true;
        }

        if (req.name() != null)     target.setName(req.name());
        if (req.lastName() != null) target.setLastName(req.lastName());
        userRepository.save(target);
        return emailChanged;
    }

    @Transactional
    public void updateUserPassword(UUID userId, String requesterEmail, UpdateUserPasswordRequest req) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!target.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("No autorizado para modificar este usuario");
        }
        if (req.currentPassword() == null || req.currentPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña actual es requerida");
        }
        if (!PasswordUtils.matches(req.currentPassword(), target.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            target.setPasswordHash(PasswordUtils.hash(req.newPassword()));
            userRepository.save(target);
        } else {
            throw new IllegalArgumentException("La nueva contraseña no puede estar vacía");
        }
    }

    @Transactional
    public void deleteUser(UUID userId, String requesterEmail) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!target.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("No autorizado para borrar este usuario");
        }
        userRepository.delete(target);
    }

    @Transactional
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}
