package com.example.bankcards.service;

import com.example.bankcards.dto.Requests.LoginRequest;
import com.example.bankcards.dto.Requests.RefreshTokenRequest;
import com.example.bankcards.dto.Response.JwtResponse;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.security.AuthService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.RefreshTokenService;
import com.example.bankcards.service.model.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider; // кастомный провайдер для генерации токенов

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;
    private CustomUserDetails userDetails;
    private Authentication authentication;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);

        userDetails = CustomUserDetails.build(user);

        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(user);
    }

    @Test
    void login_ShouldSucceed() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserById(userDetails.getId())).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn("new-refresh-token");

        // Act
        JwtResponse response = authService.login(request);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).revokeALLByUser(user);
        verify(userService).getUserById(userDetails.getId());
        verify(jwtTokenProvider).generateAccessToken(authentication);
        verify(refreshTokenService).createRefreshToken(user.getId());
    }

    @Test
    void logout_ShouldSucceed_WhenAuthenticated() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userService.getUserById(userDetails.getId())).thenReturn(user);

        // Act
        authService.logout();

        // Assert
        verify(refreshTokenService).revokeALLByUser(user);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userService).getUserById(userDetails.getId());
    }

    @Test
    void logout_ShouldDoNothing_WhenNotAuthenticated() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act
        authService.logout();

        // Assert
        verify(refreshTokenService, never()).revokeALLByUser(any());
        verify(userService, never()).getUserById(any());
    }

    @Test
    void refreshTokens_ShouldSucceed() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(refreshTokenService.findByPlainToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.isValid(refreshToken.getToken())).thenReturn(true);
        when(userService.getUserById(refreshToken.getUser().getId())).thenReturn(user);
        when(refreshTokenService.rotate("refresh-token")).thenReturn("new-refresh-token");
        when(jwtTokenProvider.generateAccessToken(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn("new-access-token");

        // Act
        JwtResponse response = authService.refreshTokens(request);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");

        verify(refreshTokenService).findByPlainToken("refresh-token");
        verify(refreshTokenService).isValid(refreshToken.getToken());
        verify(userService).getUserById(refreshToken.getUser().getId());
        verify(refreshTokenService).rotate("refresh-token");
        verify(jwtTokenProvider).generateAccessToken(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void refreshTokens_ShouldThrowException_WhenTokenNotFound() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenService.findByPlainToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshTokens(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(refreshTokenService, never()).isValid(anyString());
        verify(refreshTokenService, never()).rotate(anyString());
    }

    @Test
    void refreshTokens_ShouldThrowException_WhenTokenIsInvalid() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(refreshTokenService.findByPlainToken("expired-token")).thenReturn(Optional.of(refreshToken));
        doThrow(new RuntimeException("Refresh token expired")).when(refreshTokenService).isValid(refreshToken.getToken());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshTokens(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refresh token expired");

        verify(refreshTokenService).findByPlainToken("expired-token");
        verify(refreshTokenService).isValid(refreshToken.getToken());
        verify(refreshTokenService, never()).rotate(anyString());
    }
}
