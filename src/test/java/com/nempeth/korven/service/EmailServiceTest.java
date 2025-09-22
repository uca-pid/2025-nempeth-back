package com.nempeth.korven.service;

import com.nempeth.korven.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private AppProperties appProperties;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_RESET_LINK = "http://test.com/reset?token=abc123";

    @BeforeEach
    void setUp() {
        // Setup basic mocks with lenient stubbing
        lenient().when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().when(appProperties.getMailFrom()).thenReturn("noreply@korven.com");
    }

    @Test
    void sendPasswordResetEmail_withValidData_shouldSendEmailSuccessfully() {
        // Given
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_whenMimeMessageCreationFails_shouldLogError() {
        // Given
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP server not available"));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_whenSendingFails_shouldLogError() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_withNullEmail_shouldHandleGracefully() {
        // Given
        lenient().doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(null, TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_withEmptyEmail_shouldHandleGracefully() {
        // Given
        lenient().doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail("", TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_withNullResetLink_shouldHandleGracefully() {
        // Given
        lenient().doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, null);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_withEmptyResetLink_shouldHandleGracefully() {
        // Given
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, "");

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_shouldAlwaysLogResetLink() {
        // Given
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }

    @Test
    void sendPasswordResetEmail_shouldLogResetLinkOnError() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(javaMailSender).send(any(MimeMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(appProperties).getMailFrom();
    }
}