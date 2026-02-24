package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.StatusCard;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bank_card"
        //добавить индексы и unique constraint
)
public class Card extends BaseEntity{

    @Column(name="owner_name",nullable = false,length = 150,updatable = false)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    private StatusCard status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    //добавить аннотацию, добавляющую валидацию по **** **** **** 1234
    @Column(name = "card_number",nullable = false,unique = true)
    private String cardNumber;

    @Column(name = "expired_date",nullable = false)
    private LocalDateTime expiredDate;

    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
}
