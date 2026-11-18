package com.employeeportal.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "password_hash")
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** A user invited by an admin: no password until they activate the account. */
    public static User pending(String email, String fullName, String phone, Role role) {
        User user = new User();
        user.setEmail(email);
        user.fullName = fullName;
        user.phone = phone;
        user.role = role;
        user.status = UserStatus.PENDING;
        return user;
    }

    /** An account that can sign in immediately (used for the seeded admin). */
    public static User active(String email, String fullName, Role role, String passwordHash) {
        User user = new User();
        user.setEmail(email);
        user.fullName = fullName;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        user.passwordHash = passwordHash;
        return user;
    }

    /** Emails are case-insensitive: always stored trimmed and lowercase. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void deactivate() {
        status = UserStatus.INACTIVE;
    }

    /** An account that never set a password goes back to waiting for its invite. */
    public void reactivate() {
        status = passwordHash == null ? UserStatus.PENDING : UserStatus.ACTIVE;
    }
}
