package com.employeeportal.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Creates throwaway accounts and signs them in. Every account gets a unique email so tests that
 * share the database never see each other's data. Registered by {@link IntegrationTest}.
 */
public class TestAccounts {

    public static final String PASSWORD = "Secret123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;

    public TestAccounts(UserRepository userRepository, PasswordEncoder passwordEncoder, MockMvc mockMvc) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public User active(Role role) {
        return active(role, "Test User");
    }

    public User active(Role role, String fullName) {
        return userRepository.save(User.active(uniqueEmail("user"), fullName, role, passwordEncoder.encode(PASSWORD)));
    }

    /** Signs in on the page that matches the role and returns the full response (cookie included). */
    public MvcResult signIn(User user) throws Exception {
        String path = user.getRole() == Role.ADMIN ? "/api/admin/auth/login" : "/api/auth/login";
        return mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(user.getEmail(), PASSWORD)))
                .andReturn();
    }

    /** Signs in and returns an {@code Authorization} header value. */
    public String bearer(User user) throws Exception {
        String token = JsonPath.read(signIn(user).getResponse().getContentAsString(), "$.accessToken");
        return "Bearer " + token;
    }
}
