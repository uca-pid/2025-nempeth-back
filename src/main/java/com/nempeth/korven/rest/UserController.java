package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.UpdateMembershipRoleRequest;
import com.nempeth.korven.rest.dto.UpdateMembershipStatusRequest;
import com.nempeth.korven.rest.dto.UpdateUserProfileRequest;
import com.nempeth.korven.rest.dto.UpdateUserPasswordRequest;
import com.nempeth.korven.rest.dto.UserResponse;
import com.nempeth.korven.service.UserService;
import jakarta.validation.Valid;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication auth) {
        String userEmail = auth.getName();
        UserResponse userResponse = userService.getUserByEmail(userEmail);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId, Authentication auth) {
        String requesterEmail = auth.getName();
        UserResponse userResponse = userService.getUserById(userId, requesterEmail);
        return ResponseEntity.ok(userResponse);
    }


    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable UUID userId,
                                           @RequestBody UpdateUserProfileRequest req,
                                           Authentication auth) {
        String requesterEmail = auth.getName();
        boolean emailChanged = userService.updateUserProfile(userId, requesterEmail, req);
        if (emailChanged) {
            return ResponseEntity.ok(Map.of(
                    "message", "Usuario actualizado. Reingresá con el nuevo email para obtener un nuevo token.",
                    "emailChanged", true
            ));
        }
        return ResponseEntity.ok(Map.of("message", "Usuario actualizado", "emailChanged", false));
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<?> updatePassword(@PathVariable UUID userId,
                                            @RequestBody UpdateUserPasswordRequest req,
                                            Authentication auth) {
        String requesterEmail = auth.getName();
        userService.updateUserPassword(userId, requesterEmail, req);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada"));
    }

    @PutMapping("/businesses/{businessId}/members/{userId}/status")
    public ResponseEntity<?> updateMembershipStatus(@PathVariable UUID businessId,
                                                   @PathVariable UUID userId,
                                                   @Valid @RequestBody UpdateMembershipStatusRequest req,
                                                   Authentication auth) {
        String requesterEmail = auth.getName();
        userService.updateMembershipStatus(businessId, userId, requesterEmail, req);
        return ResponseEntity.ok(Map.of("message", "Status de membresía actualizado"));
    }

    @PutMapping("/businesses/{businessId}/members/{userId}/role")
    public ResponseEntity<?> updateMembershipRole(@PathVariable UUID businessId,
                                                 @PathVariable UUID userId,
                                                 @Valid @RequestBody UpdateMembershipRoleRequest req,
                                                 Authentication auth) {
        String requesterEmail = auth.getName();
        userService.updateMembershipRole(businessId, userId, requesterEmail, req);
        return ResponseEntity.ok(Map.of("message", "Role de membresía actualizado"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> delete(@PathVariable UUID userId, Authentication auth) {
        String requesterEmail = auth.getName();
        userService.deleteUser(userId, requesterEmail);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado"));
    }
}
