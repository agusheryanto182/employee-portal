package com.employeeportal.loginhistory;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import com.employeeportal.common.ClientInfo;

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

/** One login attempt. Append-only: rows are never updated. */
@Entity
@Immutable
@Table(name = "login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Null when the attempted email does not belong to any account. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_attempted", nullable = false)
    private String emailAttempted;

    @Column(nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    private LoginFailureReason failureReason;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static LoginHistory success(UUID userId, String email, ClientInfo client) {
        return create(userId, email, true, null, client);
    }

    static LoginHistory failure(UUID userId, String email, LoginFailureReason reason, ClientInfo client) {
        return create(userId, email, false, reason, client);
    }

    private static LoginHistory create(
            UUID userId, String email, boolean success, LoginFailureReason reason, ClientInfo client) {
        LoginHistory entry = new LoginHistory();
        entry.userId = userId;
        entry.emailAttempted = email;
        entry.success = success;
        entry.failureReason = reason;
        entry.ipAddress = client.ipAddress();
        entry.userAgent = client.userAgent();
        return entry;
    }
}
