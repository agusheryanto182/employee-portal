package com.employeeportal.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.employeeportal.support.IntegrationTest;
import com.employeeportal.support.TestAccounts;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Self-service profile, password and login history for both roles. */
@IntegrationTest
class MeIT {

    private static final String NEW_PASSWORD = "Changed123!";
    private static final Pattern REFRESH_COOKIE = Pattern.compile("refresh_token=([^;]*)");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestAccounts accounts;
    @Autowired
    UserRepository userRepository;

    private static String refreshToken(MvcResult result) {
        Matcher matcher = REFRESH_COOKIE.matcher(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String bearer(MvcResult result) throws Exception {
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private int refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", token)))
                .andReturn().getResponse().getStatus();
    }

    private ResultActions changePassword(String auth, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/me/password")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(current, next)));
    }

    // --- Profile -----------------------------------------------------------------------------

    @Test
    void bothRolesSeeTheirOwnProfile() throws Exception {
        for (Role role : Role.values()) {
            User user = accounts.active(role, "Me " + role);
            mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, accounts.bearer(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.role").value(role.name()))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }
    }

    @Test
    void profileEditChangesNameAndPhoneButNeverTheEmail() throws Exception {
        User user = accounts.active(Role.EMPLOYEE, "Old Name");
        String auth = accounts.bearer(user);

        mockMvc.perform(patch("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": " New Name ", "phone": "+62 811 000 111", "email": "hacker@example.com", "role": "ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.phone").value("+62 811 000 111"))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        mockMvc.perform(patch("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": ""}
                                """))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void invalidProfileEditsReturnFieldErrors() throws Exception {
        String auth = accounts.bearer(accounts.active(Role.EMPLOYEE));

        mockMvc.perform(patch("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "  ", "phone": "call me"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void aDeactivatedUsersStillValidAccessTokenIsRejected() throws Exception {
        User user = accounts.active(Role.EMPLOYEE);
        String auth = accounts.bearer(user);
        user = userRepository.findById(user.getId()).orElseThrow();
        user.deactivate();
        userRepository.save(user);

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isUnauthorized());
        changePassword(auth, TestAccounts.PASSWORD, NEW_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void signInIsRequired() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/login-history")).andExpect(status().isUnauthorized());
    }

    // --- Password ----------------------------------------------------------------------------

    @Test
    void changingThePasswordKeepsThisDeviceAndSignsOutTheOthers() throws Exception {
        User user = accounts.active(Role.EMPLOYEE);
        MvcResult otherDevice = accounts.signIn(user);
        MvcResult thisDevice = accounts.signIn(user);

        MvcResult changed = changePassword(bearer(thisDevice), TestAccounts.PASSWORD, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andReturn();

        assertThat(refresh(refreshToken(otherDevice))).isEqualTo(401);
        assertThat(refresh(refreshToken(thisDevice))).isEqualTo(401);
        assertThat(refresh(refreshToken(changed))).isEqualTo(200);

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(changed))).andExpect(status().isOk());
        assertThat(accounts.signIn(user).getResponse().getStatus()).isEqualTo(401); // old password
    }

    @Test
    void theCurrentPasswordMustBeCorrectAndTheNewOneDifferentAndLongEnough() throws Exception {
        String auth = accounts.bearer(accounts.active(Role.ADMIN));

        changePassword(auth, "wrong-password", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").value("is incorrect"));
        changePassword(auth, TestAccounts.PASSWORD, TestAccounts.PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
        changePassword(auth, TestAccounts.PASSWORD, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    // --- Login history -----------------------------------------------------------------------

    @Test
    void historyShowsOnlyMyOwnAttemptsNewestFirst() throws Exception {
        User me = accounts.active(Role.EMPLOYEE);
        User someoneElse = accounts.active(Role.EMPLOYEE);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "MeIT/1.0")
                .content("""
                        {"email": "%s", "password": "wrong-password"}
                        """.formatted(me.getEmail())));
        accounts.signIn(someoneElse);
        String auth = accounts.bearer(me);

        mockMvc.perform(get("/api/me/login-history").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].userId", everyItem(is(me.getId().toString()))))
                .andExpect(jsonPath("$.content[0].success").value(true))
                .andExpect(jsonPath("$.content[1].success").value(false))
                .andExpect(jsonPath("$.content[1].failureReason").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.content[1].userAgent").value("MeIT/1.0"));

        mockMvc.perform(get("/api/me/login-history").header(HttpHeaders.AUTHORIZATION, auth).param("size", "1"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
