package com.example.bankcards.security;

import com.example.bankcards.exception.JwtException.KeyNoFoundException;
import com.example.bankcards.util.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class KeyService {

    private final JwtProperties properties;
    private final ConcurrentHashMap<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private volatile KeyPair currentAccessKeyPair;
    private volatile String accessKeyId;

    public KeyService(JwtProperties properties) {
        this.properties = properties;
        this.currentAccessKeyPair = loadKeyPair(
                properties.getAccessPrivateKeyPath(),
                properties.getAccessPublicKeyPath()
        );
        this.accessKeyId = generateAccessKeyId(currentAccessKeyPair.getPublic());
        this.publicKeyCache.put(accessKeyId, currentAccessKeyPair.getPublic());
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public KeyPair getAccessKeyPair() {
        return currentAccessKeyPair;
    }

    public PublicKey getPublicKeyByKeyId(String accessKeyId) {
        PublicKey key = publicKeyCache.get(accessKeyId);
        if (key == null) {
            throw new KeyNoFoundException("Key not found by id: " + accessKeyId);
        }
        return key;
    }

    public long getAccessExpirationMs() {
        return properties.getAccessExpirationMs();
    }

    private KeyPair loadKeyPair(String privateKeyPath, String publicKeyPath) {
        try {
            String privatePem = readKeyFromFile(privateKeyPath);
            String publicPem = readKeyFromFile(publicKeyPath);

            PrivateKey privateKey = parsePrivateKey(privatePem);
            PublicKey publicKey = parsePublicKey(publicPem);

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load key pair from " + privateKeyPath + ", " + publicKeyPath, e);
        }
    }

    private String readKeyFromFile(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return new String(resource.getInputStream().readAllBytes());
        } else {
            return Files.readString(Path.of(path));
        }
    }

    private PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = extractBase64FromPem(pem, "PRIVATE KEY");
        byte[] decoded = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private PublicKey parsePublicKey(String pem) throws Exception {
        String base64 = extractBase64FromPem(pem, "PUBLIC KEY");
        byte[] decoded = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private String extractBase64FromPem(String pem, String keyType) {
        String beginMarker = "-----BEGIN " + keyType + "-----";
        String endMarker = "-----END " + keyType + "-----";
        int start = pem.indexOf(beginMarker);
        if (start == -1) {
            throw new IllegalArgumentException("PEM does not contain " + beginMarker);
        }
        int end = pem.indexOf(endMarker, start + beginMarker.length());
        if (end == -1) {
            throw new IllegalArgumentException("PEM does not contain " + endMarker);
        }
        String base64Part = pem.substring(start + beginMarker.length(), end)
                .replaceAll("\\s", ""); // удаляем все пробельные символы
        return base64Part;
    }

    private String generateAccessKeyId(PublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hash)
                    .substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}