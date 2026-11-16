package com.employeeportal.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.employeeportal.support.IntegrationTest;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;

@IntegrationTest
class AdminSeederIT {

    @Autowired
    AdminSeeder adminSeeder;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void seedsAnActiveAdminWithNormalizedEmailOnStartup() {
        User admin = userRepository.findByEmail("admin@test.example").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getFullName()).isEqualTo("Test Admin");
        assertThat(passwordEncoder.matches("Admin12345!", admin.getPasswordHash())).isTrue();
    }

    @Test
    void runningAgainDoesNotCreateADuplicate() {
        long before = userRepository.count();

        adminSeeder.run(null);

        assertThat(userRepository.count()).isEqualTo(before);
    }
}
