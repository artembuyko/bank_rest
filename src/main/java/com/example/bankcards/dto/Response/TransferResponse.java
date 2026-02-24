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
public class TransferResponse implements Serializable {
    private Long id;
    private Long sourceCardId;
    private Long targetCardId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String status;
}
