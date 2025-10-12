package com.nempeth.korven.service;

import com.nempeth.korven.constants.MembershipStatus;
import com.nempeth.korven.persistence.entity.BusinessMembership;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.BusinessMembershipRepository;
import com.nempeth.korven.persistence.repository.SaleRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final BusinessMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<MonthlyCategoryRevenueResponse> getMonthlyRevenueByCategory(String userEmail, UUID businessId, 
                                                                           Integer year) {
        validateUserBusinessAccess(userEmail, businessId);
        
        OffsetDateTime startOfYear = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, 
                OffsetDateTime.now().getOffset());
        OffsetDateTime endOfYear = OffsetDateTime.of(year, 12, 31, 23, 59, 59, 999999999, 
                OffsetDateTime.now().getOffset());
        
        List<Object[]> results = saleRepository.findMonthlyRevenueByCategory(businessId, startOfYear, endOfYear);
        
        return results.stream()
                .map(result -> MonthlyCategoryRevenueResponse.builder()
                        .month(YearMonth.of((Integer) result[0], (Integer) result[1]))
                        .categoryName((String) result[2])
                        .revenue((BigDecimal) result[3])
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyCategoryProfitResponse> getMonthlyProfitByCategory(String userEmail, UUID businessId, 
                                                                         Integer year) {
        validateUserBusinessAccess(userEmail, businessId);
        
        OffsetDateTime startOfYear = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, 
                OffsetDateTime.now().getOffset());
        OffsetDateTime endOfYear = OffsetDateTime.of(year, 12, 31, 23, 59, 59, 999999999, 
                OffsetDateTime.now().getOffset());
        
        List<Object[]> results = saleRepository.findMonthlyProfitByCategory(businessId, startOfYear, endOfYear);
        
        return results.stream()
                .map(result -> MonthlyCategoryProfitResponse.builder()
                        .month(YearMonth.of((Integer) result[0], (Integer) result[1]))
                        .categoryName((String) result[2])
                        .profit((BigDecimal) result[3])
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueResponse> getMonthlyTotalRevenue(String userEmail, UUID businessId, Integer year) {
        validateUserBusinessAccess(userEmail, businessId);
        
        OffsetDateTime startOfYear = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, 
                OffsetDateTime.now().getOffset());
        OffsetDateTime endOfYear = OffsetDateTime.of(year, 12, 31, 23, 59, 59, 999999999, 
                OffsetDateTime.now().getOffset());
        
        List<Object[]> results = saleRepository.findMonthlyTotalRevenue(businessId, startOfYear, endOfYear);
        
        return results.stream()
                .map(result -> MonthlyRevenueResponse.builder()
                        .month(YearMonth.of((Integer) result[0], (Integer) result[1]))
                        .revenue((BigDecimal) result[2])
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyProfitResponse> getMonthlyTotalProfit(String userEmail, UUID businessId, Integer year) {
        validateUserBusinessAccess(userEmail, businessId);
        
        OffsetDateTime startOfYear = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, 
                OffsetDateTime.now().getOffset());
        OffsetDateTime endOfYear = OffsetDateTime.of(year, 12, 31, 23, 59, 59, 999999999, 
                OffsetDateTime.now().getOffset());
        
        List<Object[]> results = saleRepository.findMonthlyTotalProfit(businessId, startOfYear, endOfYear);
        
        return results.stream()
                .map(result -> MonthlyProfitResponse.builder()
                        .month(YearMonth.of((Integer) result[0], (Integer) result[1]))
                        .profit((BigDecimal) result[2])
                        .build())
                .toList();
    }

    private void validateUserBusinessAccess(String userEmail, UUID businessId) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        BusinessMembership membership = membershipRepository.findByBusinessIdAndUserId(businessId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tienes acceso a este negocio"));
        
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalArgumentException("Tu membresía en este negocio no está activa");
        }
    }
}