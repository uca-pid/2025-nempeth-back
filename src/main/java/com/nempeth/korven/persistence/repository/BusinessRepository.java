package com.nempeth.korven.persistence.repository;

import com.nempeth.korven.persistence.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
    Optional<Business> findByJoinCode(String joinCode);
    
    boolean existsByJoinCode(String joinCode);
}