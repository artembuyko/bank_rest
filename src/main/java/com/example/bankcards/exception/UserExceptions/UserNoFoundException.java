package com.example.bankcards.exception.UserExceptions;

public class UserNoFoundException extends RuntimeException {
    public UserNoFoundException(String message) {
        super(message);
    }
}
