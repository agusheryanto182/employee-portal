package com.employeeportal.common;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Who is calling, for the login history. The IP comes from {@code getRemoteAddr()}, which Spring's
 * ForwardedHeaderFilter ({@code server.forward-headers-strategy=framework}) replaces with the
 * client address from {@code X-Forwarded-For} when the app runs behind Vercel and Render.
 */
public record ClientInfo(String ipAddress, String userAgent) {

    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_IP_LENGTH = 45; // longest textual IPv6 address

    public static ClientInfo from(HttpServletRequest request) {
        return new ClientInfo(
                truncate(request.getRemoteAddr(), MAX_IP_LENGTH),
                truncate(request.getHeader(HttpHeaders.USER_AGENT), MAX_USER_AGENT_LENGTH));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
