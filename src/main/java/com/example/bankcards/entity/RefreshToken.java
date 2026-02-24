package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name="refresh_token")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RefreshToken extends BaseEntity{

    @OneToOne()
    @JoinColumn(name = "user_id",referencedColumnName = "id")
    private User user;

    @Column(name = "user_agent",nullable = false)
    private String userAgent;

    @Column(name = "ip_address",nullable = false)
    private String ipAddress;

    @Column(nullable = false,unique = true)
    private String token;

    @Column(nullable = false,unique = true)
    private LocalDateTime expiryDate;

}
