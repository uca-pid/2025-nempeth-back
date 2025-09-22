package com.nempeth.korven.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** URL del frontend que tiene la pantalla de reset */
    private String frontendBaseUrl; // ej: https://korven.com.ar
    /** Remitente del email */
    private String mailFrom;        // ej: no-reply@korven.com.ar
    /** Validez del token en minutos */
    private int resetTokenTtlMinutes = 30;
}
