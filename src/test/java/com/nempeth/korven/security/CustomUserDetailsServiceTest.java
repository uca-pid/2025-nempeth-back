package com.nempeth.korven.security;

import com.nempeth.korven.config.TestMailConfiguration;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestMailConfiguration.class)
class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .name("John")
                .lastName("Doe")
                .passwordHash("hashedPassword123")
                .role(Role.USER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    void loadUserByUsername_withValidEmail_shouldReturnUserDetails() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("hashedPassword123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_withValidEmailDifferentCase_shouldReturnUserDetails() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("TEST@EXAMPLE.COM");
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("hashedPassword123", userDetails.getPassword());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_withMixedCaseEmail_shouldReturnUserDetails() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("Test@Example.Com");
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_withOwnerRole_shouldReturnOwnerAuthority() {
        User ownerUser = User.builder()
                .email("owner@example.com")
                .name("Jane")
                .lastName("Owner")
                .passwordHash("ownerPassword123")
                .role(Role.OWNER)
                .build();
        userRepository.save(ownerUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("owner@example.com");

        assertNotNull(userDetails);
        assertEquals("owner@example.com", userDetails.getUsername());
        assertEquals("ownerPassword123", userDetails.getPassword());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_OWNER")));
    }

    @Test
    void loadUserByUsername_withNonExistentEmail_shouldThrowUsernameNotFoundException() {
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent@example.com")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void loadUserByUsername_withEmptyEmail_shouldThrowUsernameNotFoundException() {
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void loadUserByUsername_withNullEmail_shouldThrowUsernameNotFoundException() {
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void loadUserByUsername_withWhitespaceEmail_shouldThrowUsernameNotFoundException() {
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("   ")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void loadUserByUsername_shouldReturnSpringUserDetailsImplementation() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        assertNotNull(userDetails);
        assertTrue(userDetails instanceof org.springframework.security.core.userdetails.User);
    }

    @Test
    void loadUserByUsername_shouldReturnCorrectUserDetailsFields() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("hashedPassword123", userDetails.getPassword());
        
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }
}