package com.employeeportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.employeeportal.loginhistory.LoginFailureReason;
import com.employeeportal.loginhistory.LoginHistory;
import com.employeeportal.loginhistory.LoginHistoryRepository;
import com.employeeportal.support.IntegrationTest;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Login, refresh, logout and login history over HTTP against a real database. */
@IntegrationTest
class AuthIT {

    private static final String PASSWORD = "Secret123!";
    private static final Pattern REFRESH_COOKIE = Pattern.compile("refresh_token=([^;]*)");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    LoginHistoryRepository loginHistoryRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtDecoder jwtDecoder;

    /** Each test uses its own user so tests never share rate-limit or history state. */
    private User createUser(Role role, UserStatus status) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        User user = status == UserStatus.PENDING
                ? User.pending(email, "Test User", null, role)
                : User.active(email, "Test User", role, passwordEncoder.encode(PASSWORD));
        user.setStatus(status);
        return userRepository.save(user);
    }

    private MvcResult login(String email, String password, String... forwardedFor) throws Exception {
        return loginAt("/api/auth/login", email, password, forwardedFor);
    }

    private MvcResult adminLogin(String email, String password) throws Exception {
        return loginAt("/api/admin/auth/login", email, password);
    }

    private MvcResult loginAt(String path, String email, String password, String... forwardedFor) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "AuthIT/1.0")
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
        if (forwardedFor.length > 0) {
            request.header("X-Forwarded-For", forwardedFor[0]);
        }
        return mockMvc.perform(request).andReturn();
    }

    private static String refreshCookieValue(MvcResult result) {
        Matcher matcher = REFRESH_COOKIE.matcher(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private List<LoginHistory> historyOf(String email) {
        return loginHistoryRepository.findByEmailAttemptedOrderByCreatedAtDesc(email);
    }

    // --- Login -------------------------------------------------------------------------------

    @Test
    void loginReturnsAccessTokenAndSecureRefreshCookie() throws Exception {
        User user = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);

        MvcResult result = login(user.getEmail(), PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String body = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(body, "$.tokenType")).isEqualTo("Bearer");
        assertThat((Integer) JsonPath.read(body, "$.expiresIn")).isEqualTo(900);
        assertThat((String) JsonPath.read(body, "$.user.email")).isEqualTo(user.getEmail());
        assertThat(body).doesNotContain("password");

        String cookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/api/auth");

        Jwt jwt = jwtDecoder.decode(accessToken(result));
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("EMPLOYEE");
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        User user = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);

        assertThat(login(user.getEmail().toUpperCase(), PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void successfulLoginIsRecordedWithClientIpAndUserAgent() throws Exception {
        User user = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);

        login(user.getEmail(), PASSWORD, "203.0.113.7");

        List<LoginHistory> history = historyOf(user.getEmail());
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.isSuccess()).isTrue();
            assertThat(entry.getUserId()).isEqualTo(user.getId());
            assertThat(entry.getFailureReason()).isNull();
            assertThat(entry.getIpAddress()).isEqualTo("203.0.113.7");
            assertThat(entry.getUserAgent()).isEqualTo("AuthIT/1.0");
            assertThat(entry.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void wrongPasswordUnknownEmailPendingAndInactiveAllGetTheSameResponse() throws Exception {
        User active = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);
        User pending = createUser(Role.EMPLOYEE, UserStatus.PENDING);
        User inactive = createUser(Role.EMPLOYEE, UserStatus.INACTIVE);
        String unknown = "ghost-" + UUID.randomUUID() + "@example.com";

        for (MvcResult result : List.of(
                login(active.getEmail(), "wrong-password"),
                login(unknown, PASSWORD),
                login(pending.getEmail(), PASSWORD),
                login(inactive.getEmail(), PASSWORD))) {
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.detail"))
                    .isEqualTo("Invalid email or password");
            assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        }

        assertThat(historyOf(active.getEmail())).singleElement()
                .satisfies(e -> assertThat(e.getFailureReason()).isEqualTo(LoginFailureReason.INVALID_CREDENTIALS));
        assertThat(historyOf(unknown)).singleElement().satisfies(e -> {
            assertThat(e.getUserId()).isNull();
            assertThat(e.getFailureReason()).isEqualTo(LoginFailureReason.INVALID_CREDENTIALS);
        });
        assertThat(historyOf(pending.getEmail())).singleElement()
                .satisfies(e -> assertThat(e.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_PENDING));
        assertThat(historyOf(inactive.getEmail())).singleElement()
                .satisfies(e -> assertThat(e.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_INACTIVE));
    }

    @Test
    void tooManyFailedAttemptsReturn429AndAreRecorded() throws Exception {
        User user = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);
        for (int i = 0; i < 5; i++) {
            login(user.getEmail(), "wrong-password");
        }

        MvcResult blocked = login(user.getEmail(), PASSWORD);

        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(blocked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(historyOf(user.getEmail()).getFirst().getFailureReason()).isEqualTo(LoginFailureReason.RATE_LIMITED);
    }

    @Test
    void adminSignsInOnTheBackofficeLoginPage() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE);

        MvcResult result = adminLogin(admin.getEmail(), PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(jwtDecoder.decode(accessToken(result)).getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Path=/api/auth");
        assertThat(historyOf(admin.getEmail())).singleElement().satisfies(e -> assertThat(e.isSuccess()).isTrue());
    }

    @Test
    void eachLoginPageRejectsTheOtherRoleWithTheGenericError() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE);
        User employee = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);

        for (MvcResult result : List.of(login(admin.getEmail(), PASSWORD), adminLogin(employee.getEmail(), PASSWORD))) {
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.detail"))
                    .isEqualTo("Invalid email or password");
            assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        }
        assertThat(historyOf(admin.getEmail())).singleElement()
                .satisfies(e -> assertThat(e.getFailureReason()).isEqualTo(LoginFailureReason.WRONG_PORTAL));
        assertThat(historyOf(employee.getEmail())).singleElement()
                .satisfies(e -> assertThat(e.getFailureReason()).isEqualTo(LoginFailureReason.WRONG_PORTAL));
    }

    @Test
    void adminRefreshAndLogoutUseTheSharedEndpoints() throws Exception {
        String refresh = refreshCookieValue(adminLogin(createUser(Role.ADMIN, UserStatus.ACTIVE).getEmail(), PASSWORD));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void invalidLoginRequestReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // --- Refresh and logout --------------------------------------------------------------------

    @Test
    void refreshRotatesTheCookieAndReturnsANewAccessToken() throws Exception {
        User user = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);
        String firstRefresh = refreshCookieValue(login(user.getEmail(), PASSWORD));

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", firstRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String secondRefresh = refreshCookieValue(refreshed);
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // The rotated token is no longer accepted, and the response clears it.
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", firstRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        // The new one still works (reuse within the grace window does not sign the user out).
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", secondRefresh)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshTokenAndClearsTheCookie() throws Exception {
        User user = createUser(Role.EMPLOYEE, UserStatus.ACTIVE);
        String refresh = refreshCookieValue(login(user.getEmail(), PASSWORD));

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", refresh)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refresh)))
                .andExpect(status().isUnauthorized());
    }

    // --- Using the access token ----------------------------------------------------------------

    @Test
    void accessTokenAuthenticatesRequestsAndRolesAreEnforced() throws Exception {
        String employeeToken = accessToken(login(createUser(Role.EMPLOYEE, UserStatus.ACTIVE).getEmail(), PASSWORD));
        String adminToken = accessToken(adminLogin(createUser(Role.ADMIN, UserStatus.ACTIVE).getEmail(), PASSWORD));

        // Authenticated: passes security and reaches routing (no such endpoint yet, so 404, not 401).
        mockMvc.perform(get("/api/does-not-exist").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isNotFound());
        // Employees are kept out of the backoffice API.
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/admin/does-not-exist").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidAccessTokenIsRejectedAsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
