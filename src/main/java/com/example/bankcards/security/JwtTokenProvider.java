package com.example.bankcards.security;

import com.example.bankcards.entity.record.ParsedToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jayway.jsonpath.JsonPath;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtTokenProvider {

    private final Clock clock;
    private final KeyService keyService;

    public String generateAccessToken(Authentication authentication){
        return generateAccessToken(authentication,keyService.getAccessKeyPair(),keyService.getAccessExpirationMs());
    }

    private String generateAccessToken(Authentication auth, KeyPair accessKeyPair, Long expirationAccessToken){
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Date now = Date.from(clock.instant());
        Date expiry = new Date(now.getTime()+expirationAccessToken);
        String keyId = keyService.getAccessKeyId();

        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .header().add("kid",keyId).and()
                .expiration(expiry)
                .signWith(accessKeyPair.getPrivate())
                .issuedAt(now)
                .subject(userDetails.getUsername())
                .claim("type","access")
                .claim("uid",userDetails.getId())
                .claim("role", userDetails.getAuthorities().iterator().next().getAuthority());

        return builder.compact();
    }

    public ParsedToken parseToken(String token){
        try {
            String kid = extractKid(token);
            PublicKey publicKey = keyService.getPublicKeyByKeyId(kid);

            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();

            return ParsedToken.valid(claims);
        }catch (ExpiredJwtException e) {
            return ParsedToken.expired(e.getClaims());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT parse error: {}", e.getMessage());
            return ParsedToken.invalid("Invalid token");
        }
    }

    private String extractKid(String token){
        String[] parts = token.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        return JsonPath.read(headerJson, "$.kid");
    }

    private Optional<String> extractClaim(String token, Function<Claims, String> resolver){
        try{
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Claims claims = new ObjectMapper().readValue(payload, Claims.class);
            return Optional.ofNullable(resolver.apply(claims));
        }catch (Exception e) {
            return Optional.empty();
        }
    }
}
