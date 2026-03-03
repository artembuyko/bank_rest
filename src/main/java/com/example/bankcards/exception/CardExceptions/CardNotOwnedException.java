package com.example.bankcards.exception.CardExceptions;

public class CardNotOwnedException extends RuntimeException {
    public CardNotOwnedException(String message) {
        super(message);
    }
}
