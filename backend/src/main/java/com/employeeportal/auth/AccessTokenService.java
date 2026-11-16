package com.employeeportal.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.employeeportal.config.AppProperties;
import com.employeeportal.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    public static final String ROLE_CLAIM = "role";
    public static final String EMAIL_CLAIM = "email";

    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;
    private final Clock clock;

    /** @param expiresInSeconds lifetime from the moment of issue, for the {@code expiresIn} response field */
    public record AccessToken(String value, Instant expiresAt, long expiresInSeconds) {
    }

    /** Subject is the user id; the role claim drives authorization ({@code ROLE_ADMIN}, {@code ROLE_EMPLOYEE}). */
    public AccessToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt, properties.jwt().accessTokenTtl().toSeconds());
    }
}
