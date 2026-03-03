package com.example.bankcards.exception.JwtException;

public class KeyNoFoundException extends RuntimeException {
    public KeyNoFoundException(String message) {
        super(message);
    }
}
