package com.example.bankcards.security;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.service.model.UserService;
import com.example.bankcards.util.JwtProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class RefreshTokenService{

    private final RefreshTokenRepository tokenRepository;
    private final UserService userService;
    private final JwtProperties  properties;
    private final Clock clock;

    public String createRefreshToken(Long userId){
        String token = UUID.randomUUID().toString();
        String tokenHash = sha256(token);

        User user = userService.getUserById(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenHash)
                .used(false)
                .expiryDate(clock.instant().plusMillis(properties.getRefreshExpirationMs()))
                .build();

        tokenRepository.save(refreshToken);
        return token;
    }

    public String rotate(String currentPlainToken){
        RefreshToken currentToken = findByPlainToken(currentPlainToken)
                .orElseThrow(()->new SecurityException("invalid refresh token"));

        if(currentToken.getExpiryDate().isBefore(clock.instant()) || currentToken.isExpired()){
            tokenRepository.delete(currentToken);
            log.info("Expired refresh token cleanup for user: {}", currentToken.getUser().getId());
            throw new SecurityException("Refresh token expired");
        }

        if(currentToken.isUsed()){
            log.error("Token reuse detected! User: {}", currentToken.getUser().getId());
            tokenRepository.deleteAllByUser(currentToken.getUser());
            throw new SecurityException("Token reuse detected. Please login again.");
        }

        currentToken.setUsed(true);
        tokenRepository.save(currentToken);
        return createRefreshToken(currentToken.getUser().getId());
    }

    public void revokeALLByUser(User user){
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();
        log.info("All tokens revoked for user: {}", user);
    }

    public boolean isValid(String plainToken){
        return findByPlainToken(plainToken).map(token->!token.isExpired() && !token.isUsed()).orElse(false);
    }

    private String sha256(String token){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Optional<RefreshToken> findByPlainToken(String plainToken){
        String hash = sha256(plainToken);
        return tokenRepository.findByToken(hash);
    }
}
