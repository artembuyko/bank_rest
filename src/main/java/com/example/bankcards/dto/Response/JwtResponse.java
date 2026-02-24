package com.example.bankcards.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse implements Serializable {
    private String accessToken;
    private String refreshToken;
    private String username;
    private List<String> roles;
}
