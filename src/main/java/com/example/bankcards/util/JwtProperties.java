package com.example.bankcards.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private long accessExpirationMs = 900000;       // 15 минут по умолчанию
    private long refreshExpirationMs = 6048000000L; // 70 дней по умолчанию

    // пути к файлам ключей
    private String accessPrivateKeyPath;
    private String accessPublicKeyPath;
}
