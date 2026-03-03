package com.example.bankcards.config;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${default.admin.password:Admin123!}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByRole(Role.ADMIN).isEmpty()) {
            User admin = new User();
            admin.setUsername(defaultAdminUsername);
            admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setName("Administrator-0");
            userRepository.save(admin);

            log.info("Created default admin user with username: {}", defaultAdminUsername);
        }
    }
}