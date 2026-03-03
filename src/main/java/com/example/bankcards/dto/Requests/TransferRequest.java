package com.example.bankcards.dto.Requests;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Validated
public class TransferRequest implements Serializable {

    @NotNull(message = "card id can not be null")
    private Long fromCardId;

    @NotNull(message = "card id can not be null")
    private Long toCardId;

    @NotNull(message = "initial balance cannot be null")
    @Positive(message = "balance must be positive")
    @Digits(integer = 15, fraction = 2, message = "invalid balance format. Max 2 decimal places allowed")
    @Max(value = 1000000000, message = "initial balance exceeds maximum limit")
    private BigDecimal amount;

    @NotNull
    private LocalDateTime timestamp = LocalDateTime.now();

    @AssertTrue(message = "Source and target cards must be different")
    public boolean isDifferentCards() {
        if (fromCardId == null || toCardId == null) return true;
        return !fromCardId.equals(toCardId);
    }
}
