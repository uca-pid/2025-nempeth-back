package com.nempeth.korven.persistence.repository;

import com.nempeth.korven.constants.MembershipStatus;
import com.nempeth.korven.persistence.entity.BusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, UUID> {
    List<BusinessMembership> findByUserIdAndStatus(UUID userId, MembershipStatus status);
    
    Optional<BusinessMembership> findByBusinessIdAndUserId(UUID businessId, UUID userId);
    
    List<BusinessMembership> findByBusinessIdAndStatus(UUID businessId, MembershipStatus status);
    
    boolean existsByBusinessIdAndUserId(UUID businessId, UUID userId);
}