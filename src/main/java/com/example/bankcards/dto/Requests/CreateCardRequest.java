package com.example.bankcards.dto.Requests;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest implements Serializable {

    @NotNull(message = "user id can not be empty")
    private Long userId;

    @NotNull(message = "expiration date is mandatory")
    @Future(message = "the expiration date must be in the future")
    private LocalDateTime expirationDate;

    @NotNull(message = "initial balance cannot be null")
    @PositiveOrZero(message = "balance must be positive or zero")
    @Digits(integer = 15, fraction = 2, message = "invalid balance format. Max 2 decimal places allowed")
    @Max(value = 1000000000, message = "initial balance exceeds maximum limit")
    private BigDecimal balance;
}
