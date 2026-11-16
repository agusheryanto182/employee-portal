package com.employeeportal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the first admin from {@code app.admin.*} so the backoffice is reachable on a fresh database.
 * Idempotent: an existing account with that email is never modified.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.admin();
        if (!admin.isConfigured()) {
            log.warn("No initial admin configured (ADMIN_EMAIL / ADMIN_PASSWORD); skipping admin seed");
            return;
        }
        String email = User.normalizeEmail(admin.email());
        if (userRepository.existsByEmail(email)) {
            log.info("Initial admin {} already exists", email);
            return;
        }
        String fullName = admin.fullName() == null || admin.fullName().isBlank() ? "Administrator" : admin.fullName();
        userRepository.save(User.active(email, fullName, Role.ADMIN, passwordEncoder.encode(admin.password())));
        log.info("Created initial admin {}", email);
    }
}
