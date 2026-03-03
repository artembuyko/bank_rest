package com.example.bankcards.security;

import com.example.bankcards.dto.Requests.LoginRequest;
import com.example.bankcards.dto.Requests.RefreshTokenRequest;
import com.example.bankcards.dto.Response.JwtResponse;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.model.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public JwtResponse login(LoginRequest request){

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userService.getUserById(userDetails.getId());

        refreshTokenService.revokeALLByUser(user);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new JwtResponse(accessToken,refreshToken,user.getUsername(), List.of(user.getRole().name()));

    }

    @Transactional
    public void logout(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userService.getUserById(userDetails.getId());
            refreshTokenService.revokeALLByUser(user);
            SecurityContextHolder.clearContext();
            log.info("User logged out: {}", user.getUsername());
        } else {
            log.warn("Logout attempted with no authentication");
        }
    }

    @Transactional
    public JwtResponse refreshTokens(RefreshTokenRequest request){

        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByPlainToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenService.isValid(refreshToken.getToken());

        User user = userService.getUserById(refreshToken.getUser().getId());

        CustomUserDetails userDetails = CustomUserDetails.build(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String NewRefreshToken = refreshTokenService.rotate(requestRefreshToken);
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);

        return new JwtResponse(accessToken,NewRefreshToken, userDetails.getUsername(),List.of(user.getRole().name()));
    }
}
