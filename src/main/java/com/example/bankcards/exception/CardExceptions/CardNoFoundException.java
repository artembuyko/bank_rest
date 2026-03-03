package com.example.bankcards.exception.CardExceptions;

public class CardNoFoundException extends RuntimeException {
    public CardNoFoundException(String message) {
        super(message);
    }
}
