package com.nempeth.korven.persistence.repository;

import com.nempeth.korven.config.TestMailConfiguration;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestMailConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User savedUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .name("John")
                .lastName("Doe")
                .passwordHash("hashedPassword123")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldSaveAndFindUserById() {
        savedUser = userRepository.save(testUser);
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("test@example.com", foundUser.get().getEmail());
        assertEquals("John", foundUser.get().getName());
        assertEquals("Doe", foundUser.get().getLastName());
        assertEquals("hashedPassword123", foundUser.get().getPasswordHash());
        assertEquals(Role.USER, foundUser.get().getRole());
    }

    @Test
    void shouldFindUserByEmailIgnoreCase() {
        savedUser = userRepository.save(testUser);
        Optional<User> foundLowerCase = userRepository.findByEmailIgnoreCase("test@example.com");
        Optional<User> foundUpperCase = userRepository.findByEmailIgnoreCase("TEST@EXAMPLE.COM");
        Optional<User> foundMixedCase = userRepository.findByEmailIgnoreCase("Test@Example.Com");
        
        assertTrue(foundLowerCase.isPresent());
        assertTrue(foundUpperCase.isPresent());
        assertTrue(foundMixedCase.isPresent());
        
        assertEquals(savedUser.getId(), foundLowerCase.get().getId());
        assertEquals(savedUser.getId(), foundUpperCase.get().getId());
        assertEquals(savedUser.getId(), foundMixedCase.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> foundUser = userRepository.findByEmailIgnoreCase("notfound@example.com");
        assertFalse(foundUser.isPresent());
    }

    @Test
    void shouldCheckIfUserExistsByEmail() {
        savedUser = userRepository.save(testUser);
        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("notfound@example.com"));
    }

    @Test
    void shouldBeCaseSensitiveForExistsByEmail() {
        savedUser = userRepository.save(testUser);
        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("TEST@EXAMPLE.COM"));
    }

    @Test
    void shouldSaveUserWithDifferentRoles() {
        User userRole = User.builder()
                .email("user@example.com")
                .passwordHash("hash123")
                .role(Role.USER)
                .build();
        
        User ownerRole = User.builder()
                .email("owner@example.com")
                .passwordHash("hash456")
                .role(Role.OWNER)
                .build();
        
        User savedUserRole = userRepository.save(userRole);
        User savedOwnerRole = userRepository.save(ownerRole);
        
        assertEquals(Role.USER, savedUserRole.getRole());
        assertEquals(Role.OWNER, savedOwnerRole.getRole());
    }

    @Test
    void shouldHandleNullOptionalFields() {
        User minimalUser = User.builder()
                .email("minimal@example.com")
                .passwordHash("hash123")
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(minimalUser);
        
        assertNotNull(savedUser.getId());
        assertEquals("minimal@example.com", savedUser.getEmail());
        assertNull(savedUser.getName());
        assertNull(savedUser.getLastName());
        assertEquals("hash123", savedUser.getPasswordHash());
        assertEquals(Role.USER, savedUser.getRole());
    }

    @Test
    void shouldUpdateExistingUser() {
        savedUser = userRepository.save(testUser);
        UUID originalId = savedUser.getId();
        savedUser.setName("Jane");
        savedUser.setLastName("Smith");
        savedUser.setRole(Role.OWNER);
        User updatedUser = userRepository.save(savedUser);
    
        assertEquals(originalId, updatedUser.getId());
        assertEquals("Jane", updatedUser.getName());
        assertEquals("Smith", updatedUser.getLastName());
        assertEquals(Role.OWNER, updatedUser.getRole());
    }

    @Test
    void shouldDeleteUser() {
        savedUser = userRepository.save(testUser);
        UUID userId = savedUser.getId();
        userRepository.delete(savedUser);
        
        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void shouldFindAllUsers() {
        User user1 = User.builder()
                .email("user1@example.com")
                .passwordHash("hash1")
                .role(Role.USER)
                .build();
        
        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash2")
                .role(Role.OWNER)
                .build();
        
        userRepository.save(user1);
        userRepository.save(user2);
        
        List<User> allUsers = userRepository.findAll();
        
        assertTrue(allUsers.size() >= 2);
        assertTrue(allUsers.stream().anyMatch(u -> "user1@example.com".equals(u.getEmail())));
        assertTrue(allUsers.stream().anyMatch(u -> "user2@example.com".equals(u.getEmail())));
    }

    @Test
    void shouldHandleUniqueEmailConstraint() {
        savedUser = userRepository.save(testUser);
        
        User duplicateEmailUser = User.builder()
                .email("test@example.com")
                .passwordHash("differentHash")
                .role(Role.OWNER)
                .build();
        
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateEmailUser);
            userRepository.flush();
        });
    }
}