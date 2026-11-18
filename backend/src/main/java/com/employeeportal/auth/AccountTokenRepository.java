package com.employeeportal.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    Optional<AccountToken> findByTokenHash(String tokenHash);

    /**
     * Consumes a token only if nobody else did first, so two requests racing with the same link
     * cannot both succeed.
     *
     * @return 1 if this call consumed the token, 0 otherwise
     */
    @Modifying(flushAutomatically = true)
    @Query("update AccountToken t set t.usedAt = :now where t.id = :id and t.usedAt is null")
    int consume(@Param("id") UUID id, @Param("now") Instant now);

    /** Invalidates every unused link of a user, for example when a newer one is sent. */
    @Modifying(flushAutomatically = true)
    @Query("update AccountToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
