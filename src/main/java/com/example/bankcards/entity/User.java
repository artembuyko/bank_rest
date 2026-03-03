package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User extends BaseEntity{

    @Column(nullable = false,unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false,length = 50)
    private String name;

    @Column(nullable = false,name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL,mappedBy = "owner")
    private Set<Card> cards = new HashSet<>();

    @OneToOne(mappedBy = "user",cascade = CascadeType.ALL,fetch = FetchType.LAZY,orphanRemoval = true)
    private RefreshToken refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
}
