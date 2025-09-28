package com.nempeth.korven.service;

import com.nempeth.korven.constants.MembershipRole;
import com.nempeth.korven.constants.MembershipStatus;
import com.nempeth.korven.exception.AuthenticationException;
import com.nempeth.korven.persistence.entity.Business;
import com.nempeth.korven.persistence.entity.BusinessMembership;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.BusinessMembershipRepository;
import com.nempeth.korven.persistence.repository.BusinessRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.BusinessResponse;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.RegisterEmployeeRequest;
import com.nempeth.korven.rest.dto.RegisterOwnerRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.rest.dto.RegistrationResponse;
import com.nempeth.korven.utils.PasswordUtils;
import com.nempeth.korven.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public UUID register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email ya registrado");
        }
        
        String hash = PasswordUtils.hash(req.password());
        User user = User.builder()
                .email(req.email())
                .name(req.name())
                .lastName(req.lastName())
                .passwordHash(hash)
                .build();

        userRepository.save(user);
        return user.getId();
    }

    @Transactional(readOnly = true)
    public String loginAndIssueToken(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new AuthenticationException("Credenciales inválidas"));
        if (!PasswordUtils.matches(req.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Credenciales inválidas");
        }
        
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString()
        );
        return jwtUtils.generateToken(user.getEmail(), claims);
    }

    @Transactional
    public RegistrationResponse registerOwner(RegisterOwnerRequest request) {
        // Validar que el email no exista
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email ya registrado");
        }

        // Crear el usuario
        String hash = PasswordUtils.hash(request.password());
        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .lastName(request.lastName())
                .passwordHash(hash)
                .build();

        user = userRepository.save(user);

        // Crear el negocio
        String joinCode = generateJoinCode();
        Business business = Business.builder()
                .name(request.businessName())
                .joinCode(joinCode)
                .joinCodeEnabled(true)
                .build();
        
        business = businessRepository.save(business);

        // Crear la membresía como OWNER
        BusinessMembership membership = BusinessMembership.builder()
                .user(user)
                .business(business)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();

        businessMembershipRepository.save(membership);

        BusinessResponse businessResponse = BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .build();

        return RegistrationResponse.builder()
                .userId(user.getId())
                .message("Propietario y negocio registrados exitosamente")
                .business(businessResponse)
                .build();
    }

    @Transactional
    public RegistrationResponse registerEmployee(RegisterEmployeeRequest request) {
        // Validar que el email no exista
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email ya registrado");
        }

        // Buscar el negocio por código de acceso
        Business business = businessRepository.findByJoinCode(request.businessJoinCode())
                .orElseThrow(() -> new IllegalArgumentException("Código de negocio inválido"));

        if (!business.getJoinCodeEnabled()) {
            throw new IllegalArgumentException("El negocio no está aceptando nuevos empleados");
        }

        // Crear el usuario
        String hash = PasswordUtils.hash(request.password());
        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .lastName(request.lastName())
                .passwordHash(hash)
                .build();

        user = userRepository.save(user);

        // Crear la membresía como EMPLOYEE
        BusinessMembership membership = BusinessMembership.builder()
                .user(user)
                .business(business)
                .role(MembershipRole.EMPLOYEE)
                .status(MembershipStatus.PENDING)
                .build();

        businessMembershipRepository.save(membership);

        BusinessResponse businessResponse = BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .build();

        return RegistrationResponse.builder()
                .userId(user.getId())
                .message("Empleado registrado y agregado al negocio exitosamente")
                .business(businessResponse)
                .build();
    }

    private String generateJoinCode() {
        // Generar código alfanumérico de 8 caracteres
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }
}
