package com.example.bankcards.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse implements Serializable {
    private Long id;
    private String maskNumber;
    private String ownerName;
    private BigDecimal balance;
    private String status;
    private LocalDateTime expirationDate;
    private String cardNumber;
}
