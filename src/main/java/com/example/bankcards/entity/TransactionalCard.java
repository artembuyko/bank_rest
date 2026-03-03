package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.StatusTransaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionalCard extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_card",nullable = false)
    private Card sourceCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_card",nullable = false)
    private Card targetCard;

    @Column(name = "time_stamp",nullable = false,updatable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransaction status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;


}
