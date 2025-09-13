package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.UpdateUserRequest;
import com.nempeth.korven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/{userId}")
    public ResponseEntity<?> update(@PathVariable UUID userId,
                                    @RequestBody UpdateUserRequest req,
                                    Authentication auth) {
        String requesterEmail = auth.getName();
        boolean emailChanged = userService.updateUser(userId, requesterEmail, req);

        if (emailChanged) {
            return ResponseEntity.ok(Map.of(
                    "message", "Usuario actualizado. Reingresá con el nuevo email para obtener un nuevo token.",
                    "emailChanged", true
            ));
        }
        return ResponseEntity.ok(Map.of("message", "Usuario actualizado", "emailChanged", false));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> delete(@PathVariable UUID userId, Authentication auth) {
        String requesterEmail = auth.getName();
        userService.deleteUser(userId, requesterEmail);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado"));
    }
}
