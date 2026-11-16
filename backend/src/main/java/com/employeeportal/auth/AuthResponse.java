package com.employeeportal.auth;

import com.employeeportal.user.UserResponse;

/**
 * @param expiresIn access token lifetime in seconds
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
