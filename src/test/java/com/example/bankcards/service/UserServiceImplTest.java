package com.example.bankcards.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.bankcards.dto.Mappers.UserMapper;
import com.example.bankcards.dto.Requests.CreateUserRequest;
import com.example.bankcards.dto.Response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.exception.UserExceptions.UserAlreadyExists;
import com.example.bankcards.exception.UserExceptions.UserNoFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User createUser(Long id, String username, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);
        return user;
    }

    private CreateUserRequest createUserRequest(String username, String password) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void createUser_ShouldSucceed() {
        // Arrange
        String username = "newuser";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        CreateUserRequest request = createUserRequest(username, rawPassword);
        User userBeforeSave = new User(); // то, что вернёт mapper
        User savedUser = createUser(1L, username, UserStatus.ACTIVE);
        UserResponse expectedResponse = new UserResponse();

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(userBeforeSave);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(userBeforeSave)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        // Act
        UserResponse result = userService.createUser(request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);

        verify(userRepository).existsByUsername(username);
        verify(userMapper).toUser(request);
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(user ->
                user.getStatus() == UserStatus.ACTIVE &&
                        user.getPassword().equals(encodedPassword)
        ));
        verify(userMapper).toResponse(savedUser);
    }

    @Test
    void createUser_ShouldThrowException_WhenUsernameAlreadyExists() {
        // Arrange
        String username = "existing";
        CreateUserRequest request = createUserRequest(username, "pass");
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExists.class)
                .hasMessage("User with username already exists: " + username);

        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUser(any());
    }


    @Test
    void getAllUser_ShouldReturnPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        User user1 = createUser(1L, "user1", UserStatus.ACTIVE);
        User user2 = createUser(2L, "user2", UserStatus.BLOCKED);
        Page<User> userPage = new PageImpl<>(List.of(user1, user2));

        UserResponse response1 = new UserResponse();
        UserResponse response2 = new UserResponse();

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user1)).thenReturn(response1);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        // Act
        Page<UserResponse> result = userService.getAllUser(pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(response1, response2);
        verify(userRepository).findAll(pageable);
        verify(userMapper, times(2)).toResponse(any(User.class));
    }


    @Test
    void blockUser_ShouldSucceed_WhenUserActive() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.blockUser(userId);

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(userRepository).save(user);
    }

    @Test
    void blockUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.blockUser(userId))
                .isInstanceOf(UserNoFoundException.class)
                .hasMessage("User with ID " + userId + " not found");
    }

    @Test
    void blockUser_ShouldThrowException_WhenUserNotActive() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.BLOCKED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.blockUser(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not active and cannot be blocked");

        verify(userRepository, never()).save(any());
    }

    @Test
    void activateUser_ShouldSucceed_WhenUserNotActive() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.BLOCKED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.activateUser(userId);

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.activateUser(userId))
                .isInstanceOf(UserNoFoundException.class)
                .hasMessage("User with ID " + userId + " not found");
    }

    @Test
    void activateUser_ShouldThrowException_WhenUserAlreadyActive() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.activateUser(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already active");

        verify(userRepository, never()).save(any());
    }


    @Test
    void deleteUser_ShouldSucceed_WhenUserNotDeleted() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(userId);

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETE);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNoFoundException.class)
                .hasMessage("User with ID " + userId + " not found");
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserAlreadyDeleted() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.DELETE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already deleted");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "user", UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserById(userId);

        // Assert
        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserById_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNoFoundException.class)
                .hasMessage("User with ID " + userId + " not found");
    }


    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Arrange
        String username = "testuser";
        User user = createUser(1L, username, UserStatus.ACTIVE);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        User result = userService.findByUsername(username);

        // Assert
        assertThat(result).isEqualTo(user);
    }

    @Test
    void findByUsername_ShouldThrowException_WhenNotFound() {
        // Arrange
        String username = "unknown";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByUsername(username))
                .isInstanceOf(UserNoFoundException.class)
                .hasMessage("User not found: " + username);
    }
}
