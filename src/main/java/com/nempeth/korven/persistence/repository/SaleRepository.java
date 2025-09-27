package com.nempeth.korven.persistence.repository;

import com.nempeth.korven.persistence.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
    List<Sale> findByBusinessIdOrderByOccurredAtDesc(UUID businessId);
    
    List<Sale> findByBusinessIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        UUID businessId, 
        OffsetDateTime startDate, 
        OffsetDateTime endDate
    );
    
    @Query("SELECT s FROM Sale s WHERE s.business.id = :businessId AND s.occurredAt >= :startDate ORDER BY s.occurredAt DESC")
    List<Sale> findRecentSalesForBusiness(@Param("businessId") UUID businessId, @Param("startDate") OffsetDateTime startDate);
}