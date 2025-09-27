package com.nempeth.korven.service;

import com.nempeth.korven.constants.MembershipRole;
import com.nempeth.korven.constants.MembershipStatus;
import com.nempeth.korven.persistence.entity.Business;
import com.nempeth.korven.persistence.entity.BusinessMembership;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.BusinessRepository;
import com.nempeth.korven.persistence.repository.BusinessMembershipRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.BusinessResponse;
import com.nempeth.korven.rest.dto.CreateBusinessRequest;
import com.nempeth.korven.rest.dto.JoinBusinessRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int JOIN_CODE_LENGTH = 8;

    @Transactional
    public BusinessResponse createBusiness(String userEmail, CreateBusinessRequest request) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Generar código único
        String joinCode = generateUniqueJoinCode();
        
        Business business = Business.builder()
                .name(request.name())
                .joinCode(joinCode)
                .joinCodeEnabled(true)
                .build();
        
        business = businessRepository.save(business);
        
        // Crear membership como OWNER
        BusinessMembership membership = BusinessMembership.builder()
                .business(business)
                .user(user)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
        
        membershipRepository.save(membership);
        
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .build();
    }

    @Transactional
    public BusinessResponse joinBusiness(String userEmail, JoinBusinessRequest request) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        Business business = businessRepository.findByJoinCode(request.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("Código de acceso inválido"));
        
        if (!business.getJoinCodeEnabled()) {
            throw new IllegalArgumentException("El código de acceso está deshabilitado");
        }
        
        // Verificar si ya es miembro
        if (membershipRepository.existsByBusinessIdAndUserId(business.getId(), user.getId())) {
            throw new IllegalArgumentException("Ya eres miembro de este negocio");
        }
        
        // Crear membership como EMPLOYEE
        BusinessMembership membership = BusinessMembership.builder()
                .business(business)
                .user(user)
                .role(MembershipRole.EMPLOYEE)
                .status(MembershipStatus.ACTIVE)
                .build();
        
        membershipRepository.save(membership);
        
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .build();
    }

    private String generateUniqueJoinCode() {
        String joinCode;
        do {
            joinCode = generateRandomJoinCode();
        } while (businessRepository.existsByJoinCode(joinCode));
        
        return joinCode;
    }

    private String generateRandomJoinCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        return sb.toString();
    }
}