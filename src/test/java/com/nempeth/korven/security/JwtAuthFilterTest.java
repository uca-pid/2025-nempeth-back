package com.nempeth.korven.security;

import com.nempeth.korven.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @Mock
    private Jws<Claims> jwsClaims;

    private JwtAuthFilter jwtAuthFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        jwtAuthFilter = new JwtAuthFilter(jwtUtils, userDetailsService);
        SecurityContextHolder.clearContext();
        responseWriter = new StringWriter();
    }

    @Test
    void doFilterInternal_withValidToken_shouldSetAuthentication() throws Exception {
        String token = "valid.jwt.token";
        String email = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.parseToken(token)).thenReturn(jwsClaims);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn(email);
        
        UserDetails userDetails = new User(email, "password", 
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(email);
    }

    @Test
    void doFilterInternal_withNoAuthorizationHeader_shouldContinueFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    void doFilterInternal_withEmptyAuthorizationHeader_shouldContinueFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    void doFilterInternal_withNonBearerToken_shouldContinueFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic username:password");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    void doFilterInternal_withInvalidToken_shouldReturnUnauthorized() throws Exception {
        String token = "invalid.jwt.token";
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);
        when(jwtUtils.parseToken(token)).thenThrow(new JwtException("Invalid token"));
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertTrue(responseWriter.toString().contains("Token inválido o expirado"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withExpiredToken_shouldReturnUnauthorized() throws Exception {
        String token = "expired.jwt.token";
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);
        when(jwtUtils.parseToken(token)).thenThrow(new JwtException("Token expired"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertTrue(responseWriter.toString().contains("Token inválido o expirado"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNullEmailInToken_shouldContinueFilter() throws Exception {
        String token = "valid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.parseToken(token)).thenReturn(jwsClaims);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn(null);
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_withEmptyEmailInToken_shouldReturnUnauthorized() throws Exception {
        String token = "valid.jwt.token";
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);
        when(jwtUtils.parseToken(token)).thenReturn(jwsClaims);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("");
        when(userDetailsService.loadUserByUsername(""))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertTrue(responseWriter.toString().contains("Token inválido o expirado"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withExistingAuthentication_shouldNotOverwrite() throws Exception {
        String token = "valid.jwt.token";
        String email = "test@example.com";
        
        UserDetails existingUser = new User("existing@example.com", "password", 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        existingUser, null, existingUser.getAuthorities()));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.parseToken(token)).thenReturn(jwsClaims);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn(email);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals("existing@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_withUserNotFound_shouldReturnUnauthorized() throws Exception {
        String token = "valid.jwt.token";
        String email = "nonexistent@example.com";
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);
        when(jwtUtils.parseToken(token)).thenReturn(jwsClaims);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertTrue(responseWriter.toString().contains("Token inválido o expirado"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withValidTokenAndUserDetails_shouldSetAuthenticationDetails() throws Exception {
        String token = "valid.jwt.token";
        String email = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        
        when(jwtUtils.parseToken(token)).thenReturn(jwsClaims);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn(email);
        
        UserDetails userDetails = new User(email, "password", 
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication().getDetails());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withEmptyToken_shouldReturnUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        PrintWriter printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
        when(jwtUtils.parseToken("")).thenThrow(new JwtException("Empty token"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertTrue(responseWriter.toString().contains("Token inválido o expirado"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withRuntimeException_shouldReturnUnauthorized() throws Exception {
        String token = "valid.jwt.token";
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);
        when(jwtUtils.parseToken(token)).thenThrow(new RuntimeException("Unexpected error"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertTrue(responseWriter.toString().contains("Token inválido o expirado"));
        verify(filterChain, never()).doFilter(request, response);
    }
}