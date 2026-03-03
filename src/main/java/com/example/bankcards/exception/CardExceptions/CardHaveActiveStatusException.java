package com.example.bankcards.exception.CardExceptions;

public class CardHaveActiveStatusException extends RuntimeException {
    public CardHaveActiveStatusException(String message) {
        super(message);
    }
}
