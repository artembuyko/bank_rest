package com.example.bankcards.util;

import java.security.SecureRandom;
import java.util.Random;


public class GeneratorCardNumber {
    private static final Random RANDOM = new SecureRandom();
    private static final int CARD_NUMBER_LENGTH = 16;

    public static String generate() {
        StringBuilder sb = new StringBuilder(CARD_NUMBER_LENGTH);

        sb.append(RANDOM.nextInt(9) + 1);

        for (int i = 1; i < CARD_NUMBER_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
