package com.employeeportal.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Callers pass an already normalized email (see {@link User#normalizeEmail}). */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
