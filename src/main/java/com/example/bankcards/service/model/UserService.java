package com.example.bankcards.service.model;

import com.example.bankcards.dto.Requests.CreateUserRequest;
import com.example.bankcards.dto.Response.UserResponse;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    Page<UserResponse> getAllUser(Pageable pageable);
    void blockUser(Long userId);
    void activateUser(Long userId);
    void deleteUser(Long userId);
    User getUserById(Long userId);
    User findByUsername(String username);
}
