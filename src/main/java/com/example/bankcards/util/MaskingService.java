package com.example.bankcards.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MaskingService {

    private final EncryptionService encryptionService;

    public String mask(String encryptNumber){
        String cardNumber = encryptionService.decrypt(encryptNumber);
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
