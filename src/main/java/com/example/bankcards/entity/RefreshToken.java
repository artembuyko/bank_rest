package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name="refresh_token")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RefreshToken extends BaseEntity{

    @OneToOne()
    @JoinColumn(name = "user_id",referencedColumnName = "id")
    private User user;

    @Column(nullable = false,unique = true)
    private String token;

    @Column(nullable = false)
    private boolean used;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

}
