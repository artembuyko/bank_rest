package com.example.bankcards.service.Impl;

import com.example.bankcards.dto.Mappers.UserMapper;
import com.example.bankcards.dto.Requests.CreateUserRequest;
import com.example.bankcards.dto.Response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.exception.UserExceptions.UserAlreadyExists;
import com.example.bankcards.exception.UserExceptions.UserNoFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.model.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user by admin: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExists("User with username already exists: " + request.getUsername());
        }
        User user = mapper.toUser(request);
        user.setStatus(UserStatus.ACTIVE);
        user.setPassword(encoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        log.info("User created with ID: {}, role: {}", savedUser.getId(), savedUser.getRole());
        return mapper.toResponse(savedUser);
    }

    @Override
    public Page<UserResponse> getAllUser(Pageable pageable) {
        log.info("Requesting all users, pagination: {}", pageable);
        return userRepository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void blockUser(Long userId) {
        log.info("Blocking user ID: {}", userId);
        User user = getUserEntityById(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User is not active and cannot be blocked");
        }
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        log.info("User {} blocked", userId);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        log.info("Activating user ID: {}", userId);
        User user = getUserEntityById(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalStateException("User is already active");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User {} activated", userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Soft deleting user ID: {}", userId);
        User user = getUserEntityById(userId);
        if (user.getStatus() == UserStatus.DELETE) {
            throw new IllegalStateException("User is already deleted");
        }
        user.setStatus(UserStatus.DELETE);
        userRepository.save(user);
        log.info("User {} marked as deleted", userId);
    }

    @Override
    public User getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return getUserEntityById(id);
    }

    @Override
    public User findByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNoFoundException("User not found: " + username));
    }

    private User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNoFoundException("User with ID " + id + " not found"));
    }
}