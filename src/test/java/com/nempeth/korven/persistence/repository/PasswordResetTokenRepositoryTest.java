package com.nempeth.korven.persistence.repository;

import com.nempeth.korven.config.TestMailConfiguration;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.PasswordResetToken;
import com.nempeth.korven.persistence.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestMailConfiguration.class)
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private PasswordResetToken testToken;
    private OffsetDateTime now;
    private OffsetDateTime futureTime;
    private OffsetDateTime pastTime;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now(ZoneOffset.UTC);
        futureTime = now.plusHours(1);
        pastTime = now.minusHours(1);

        testUser = User.builder()
                .email("test@example.com")
                .name("John")
                .lastName("Doe")
                .passwordHash("hashedPassword123")
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);

        testToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("test-reset-token-123")
                .expiresAt(futureTime)
                .usedAt(null)
                .createdAt(now)
                .build();
    }

    @Test
    void shouldSaveAndFindTokenById() {
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository.findById(savedToken.getId());
        
        assertTrue(foundToken.isPresent());
        assertEquals(savedToken.getId(), foundToken.get().getId());
        assertEquals("test-reset-token-123", foundToken.get().getToken());
        assertEquals(futureTime, foundToken.get().getExpiresAt());
        assertNull(foundToken.get().getUsedAt());
        assertEquals(now, foundToken.get().getCreatedAt());
        assertEquals(testUser.getId(), foundToken.get().getUser().getId());
    }

    @Test
    void shouldSaveTokenWithManuallyAssignedUuid() {
        assertNotNull(testToken.getId());
        UUID originalId = testToken.getId();        
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        
        assertNotNull(savedToken.getId());
        assertEquals(originalId, savedToken.getId());
        assertTrue(savedToken.getId() instanceof UUID);
    }

    @Test
    void shouldFindTokenByTokenString() {
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository.findByToken("test-reset-token-123");
        Optional<PasswordResetToken> notFoundToken = passwordResetTokenRepository.findByToken("non-existent-token");
        
        assertTrue(foundToken.isPresent());
        assertEquals(savedToken.getId(), foundToken.get().getId());
        assertEquals("test-reset-token-123", foundToken.get().getToken());
        assertFalse(notFoundToken.isPresent());
    }

    @Test
    void shouldHandleCaseSensitiveTokenSearch() {
        passwordResetTokenRepository.save(testToken);
        Optional<PasswordResetToken> exactMatch = passwordResetTokenRepository.findByToken("test-reset-token-123");
        Optional<PasswordResetToken> upperCase = passwordResetTokenRepository.findByToken("TEST-RESET-TOKEN-123");
        Optional<PasswordResetToken> mixedCase = passwordResetTokenRepository.findByToken("Test-Reset-Token-123");
        assertTrue(exactMatch.isPresent());
        assertFalse(upperCase.isPresent());
        assertFalse(mixedCase.isPresent());
    }

    @Test
    void shouldHandleUniqueTokenConstraint() {
        passwordResetTokenRepository.save(testToken);
        User anotherUser = User.builder()
                .email("another@example.com")
                .passwordHash("hash456")
                .role(Role.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);
        
        PasswordResetToken duplicateToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(anotherUser)
                .token("test-reset-token-123")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        
        assertThrows(Exception.class, () -> {
            passwordResetTokenRepository.save(duplicateToken);
            passwordResetTokenRepository.flush();
        });
    }

    @Test
    void shouldSaveTokenWithUsedTimestamp() {
        OffsetDateTime usedTime = now.plusMinutes(30);
        testToken.setUsedAt(usedTime);
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        
        assertEquals(usedTime, savedToken.getUsedAt());
        assertTrue(savedToken.isUsed());
    }

    @Test
    void shouldSaveExpiredToken() {
        testToken.setExpiresAt(pastTime);
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        
        assertEquals(pastTime, savedToken.getExpiresAt());
        assertTrue(savedToken.isExpired());
    }

    @Test
    void shouldUpdateExistingToken() {
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        UUID originalId = savedToken.getId();
        OffsetDateTime usedTime = OffsetDateTime.now();
        savedToken.setUsedAt(usedTime);
        PasswordResetToken updatedToken = passwordResetTokenRepository.save(savedToken);
        
        assertEquals(originalId, updatedToken.getId());
        assertEquals(usedTime, updatedToken.getUsedAt());
        assertTrue(updatedToken.isUsed());
    }

    @Test
    void shouldDeleteToken() {
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);
        UUID tokenId = savedToken.getId();
        passwordResetTokenRepository.delete(savedToken);
        Optional<PasswordResetToken> deletedToken = passwordResetTokenRepository.findById(tokenId);
        assertFalse(deletedToken.isPresent());
        
        Optional<PasswordResetToken> deletedByString = passwordResetTokenRepository.findByToken("test-reset-token-123");
        assertFalse(deletedByString.isPresent());
    }

    @Test
    void shouldHandleMultipleTokensForSameUser() {
        PasswordResetToken token1 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("token-1-for-user")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        
        PasswordResetToken token2 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("token-2-for-user")
                .expiresAt(futureTime.plusHours(1))
                .createdAt(now.plusMinutes(5))
                .build();
        PasswordResetToken savedToken1 = passwordResetTokenRepository.save(token1);
        PasswordResetToken savedToken2 = passwordResetTokenRepository.save(token2);
        assertNotEquals(savedToken1.getId(), savedToken2.getId());
        assertEquals(testUser.getId(), savedToken1.getUser().getId());
        assertEquals(testUser.getId(), savedToken2.getUser().getId());
        
        Optional<PasswordResetToken> foundToken1 = passwordResetTokenRepository.findByToken("token-1-for-user");
        Optional<PasswordResetToken> foundToken2 = passwordResetTokenRepository.findByToken("token-2-for-user");
        
        assertTrue(foundToken1.isPresent());
        assertTrue(foundToken2.isPresent());
    }

    @Test
    void shouldHandleTokensForDifferentUsers() {
        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash789")
                .role(Role.OWNER)
                .build();
        user2 = userRepository.save(user2);
        
        PasswordResetToken token1 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("token-for-user1")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        
        PasswordResetToken token2 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .token("token-for-user2")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        
        PasswordResetToken savedToken1 = passwordResetTokenRepository.save(token1);
        PasswordResetToken savedToken2 = passwordResetTokenRepository.save(token2);
        
        assertEquals(testUser.getId(), savedToken1.getUser().getId());
        assertEquals(user2.getId(), savedToken2.getUser().getId());
        assertEquals(Role.USER, savedToken1.getUser().getRole());
        assertEquals(Role.OWNER, savedToken2.getUser().getRole());
    }

    @Test
    void shouldCountTokensCorrectly() {
        long initialCount = passwordResetTokenRepository.count();
        
        passwordResetTokenRepository.save(testToken);
        
        PasswordResetToken anotherToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("another-token-789")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        passwordResetTokenRepository.save(anotherToken);
        long newCount = passwordResetTokenRepository.count();
        assertEquals(initialCount + 2, newCount);
    }

    @Test
    void shouldFindAllTokens() {
        PasswordResetToken token1 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("find-all-token-1")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        
        PasswordResetToken token2 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("find-all-token-2")
                .expiresAt(futureTime)
                .createdAt(now)
                .build();
        
        passwordResetTokenRepository.save(token1);
        passwordResetTokenRepository.save(token2);
        
        List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll();
        
        assertTrue(allTokens.size() >= 2);
        assertTrue(allTokens.stream().anyMatch(t -> "find-all-token-1".equals(t.getToken())));
        assertTrue(allTokens.stream().anyMatch(t -> "find-all-token-2".equals(t.getToken())));
    }

    @Test
    void shouldHandleTokenLifecycleCorrectly() {
        PasswordResetToken lifecycleToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("lifecycle-token")
                .expiresAt(futureTime)
                .usedAt(null)
                .createdAt(now)
                .build();
        PasswordResetToken savedToken = passwordResetTokenRepository.save(lifecycleToken);
        assertFalse(savedToken.isUsed());
        assertFalse(savedToken.isExpired());
        savedToken.setUsedAt(OffsetDateTime.now());
        PasswordResetToken usedToken = passwordResetTokenRepository.save(savedToken);
        assertTrue(usedToken.isUsed());
        assertFalse(usedToken.isExpired());
        usedToken.setExpiresAt(pastTime);
        PasswordResetToken expiredToken = passwordResetTokenRepository.save(usedToken);
        assertTrue(expiredToken.isUsed());
        assertTrue(expiredToken.isExpired());
    }
}