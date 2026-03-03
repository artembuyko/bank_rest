package com.example.bankcards.entity.record;

import io.jsonwebtoken.Claims;
import java.util.Date;

public record ParsedToken(
        boolean valid,
        boolean expired,
        String error,
        String subject,
        long userId,
        String role,
        String jti,
        Date expiry
) {
    public static ParsedToken valid(Claims claims) {
        return new ParsedToken(
                true, false, null,
                claims.getSubject(),
                claims.get("uid", Long.class),
                claims.get("role", String.class),
                claims.getId(),
                claims.getExpiration()
        );
    }

    public static ParsedToken expired(Claims claims) {
        return new ParsedToken(false, true, "Token expired", null, 0, null, null, null);
    }

    public static ParsedToken invalid(String reason) {
        return new ParsedToken(false, false, reason, null, 0, null, null, null);
    }
}