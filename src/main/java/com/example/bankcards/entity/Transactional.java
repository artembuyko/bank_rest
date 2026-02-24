package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Transactional extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_card",nullable = false)
    private Card sourceCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_card",nullable = false)
    private Card currentCard;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;


}
