package com.employeeportal.auth;

import static com.employeeportal.support.TestAccounts.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.employeeportal.support.IntegrationTest;
import com.employeeportal.support.TestAccounts;
import com.employeeportal.support.TestMailbox.Inbox;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.jayway.jsonpath.JsonPath;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;

/** Invite and password-reset links, end to end: admin action, email, link, new password, sign in. */
@IntegrationTest
class AccountTokenIT {

    private static final String NEW_PASSWORD = "BrandNew123!";
    private static final Pattern REFRESH_COOKIE = Pattern.compile("refresh_token=([^;]*)");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestAccounts accounts;
    @Autowired
    Inbox inbox;

    String adminAuth;

    @BeforeEach
    void signInAsAdmin() throws Exception {
        adminAuth = accounts.bearer(accounts.active(Role.ADMIN));
    }

    private UUID invite(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "Invited Person", "role": "EMPLOYEE"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private ResultActions adminAction(UUID id, String action) throws Exception {
        return mockMvc.perform(post("/api/admin/users/{id}/{action}", id, action).header(HttpHeaders.AUTHORIZATION, adminAuth));
    }

    private ResultActions setPassword(String path, String token, String password) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "password": "%s"}
                        """.formatted(token, password)));
    }

    private int signIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void invitedEmployeeActivatesWithTheEmailedLinkAndSignsIn() throws Exception {
        String email = uniqueEmail("invited");
        invite(email);

        MimeMessage message = inbox.awaitMessage(email, 1);
        assertThat(message.getSubject()).isEqualTo("You're invited to Employee Portal");
        assertThat(message.getFrom()[0].toString()).contains("no-reply@");
        assertThat(Inbox.html(message)).contains("Invited Person", "24 hours");
        assertThat(Inbox.link(message)).startsWith("http://localhost:5173/activate?token=");
        String token = Inbox.token(message);

        mockMvc.perform(get("/api/auth/tokens/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INVITE"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Invited Person"));

        assertThat(signIn(email, NEW_PASSWORD)).isEqualTo(401); // still PENDING
        setPassword("/api/auth/activate", token, NEW_PASSWORD).andExpect(status().isNoContent());
        assertThat(signIn(email, NEW_PASSWORD)).isEqualTo(200);

        // The link works only once.
        setPassword("/api/auth/activate", token, "Another123!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This link is invalid or has expired"));
        mockMvc.perform(get("/api/auth/tokens/{token}", token)).andExpect(status().isBadRequest());
    }

    @Test
    void resendingAnInviteReturnsTheLinkAndCancelsTheOldOne() throws Exception {
        String email = uniqueEmail("resend");
        UUID id = invite(email);
        String firstToken = Inbox.token(inbox.awaitMessage(email, 1));

        String link = JsonPath.read(adminAction(id, "resend-invite")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.link");
        String secondToken = Inbox.token(inbox.awaitMessage(email, 2));

        assertThat(link).endsWith("token=" + secondToken);
        mockMvc.perform(get("/api/auth/tokens/{token}", firstToken)).andExpect(status().isBadRequest());
        setPassword("/api/auth/activate", secondToken, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void deactivatingAnInvitedUserCancelsTheInvite() throws Exception {
        String email = uniqueEmail("cancelled");
        UUID id = invite(email);
        String token = Inbox.token(inbox.awaitMessage(email, 1));

        adminAction(id, "deactivate").andExpect(status().isOk());

        setPassword("/api/auth/activate", token, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void changingAPendingUsersEmailSendsTheInviteToTheNewAddress() throws Exception {
        String oldEmail = uniqueEmail("typo");
        String newEmail = uniqueEmail("fixed");
        UUID id = invite(oldEmail);
        String oldToken = Inbox.token(inbox.awaitMessage(oldEmail, 1));

        mockMvc.perform(patch("/api/admin/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(newEmail)))
                .andExpect(status().isOk());

        String newToken = Inbox.token(inbox.awaitMessage(newEmail, 1));
        mockMvc.perform(get("/api/auth/tokens/{token}", oldToken)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/auth/tokens/{token}", newToken))
                .andExpect(jsonPath("$.email").value(newEmail));
    }

    @Test
    void passwordResetReplacesThePasswordAndSignsOutEveryDevice() throws Exception {
        User user = accounts.active(Role.EMPLOYEE);
        MvcResult session = accounts.signIn(user);
        Matcher cookie = REFRESH_COOKIE.matcher(session.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(cookie.find()).isTrue();

        String link = JsonPath.read(adminAction(user.getId(), "reset-password")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.link");
        MimeMessage message = inbox.awaitMessage(user.getEmail(), 1);
        assertThat(message.getSubject()).isEqualTo("Reset your Employee Portal password");
        assertThat(Inbox.html(message)).contains("1 hour");
        assertThat(link).startsWith("http://localhost:5173/reset-password?token=");

        // The old password keeps working until the link is used.
        assertThat(signIn(user.getEmail(), TestAccounts.PASSWORD)).isEqualTo(200);

        setPassword("/api/auth/reset-password", Inbox.token(message), NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(signIn(user.getEmail(), TestAccounts.PASSWORD)).isEqualTo(401);
        assertThat(signIn(user.getEmail(), NEW_PASSWORD)).isEqualTo(200);
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", cookie.group(1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linksOnlyWorkForTheirPurposeAndStatus() throws Exception {
        User active = accounts.active(Role.EMPLOYEE);
        adminAction(active.getId(), "resend-invite")
                .andExpect(status().isConflict());

        String email = uniqueEmail("pending");
        UUID pendingId = invite(email);
        adminAction(pendingId, "reset-password")
                .andExpect(status().isConflict());

        String inviteToken = Inbox.token(inbox.awaitMessage(email, 1));
        setPassword("/api/auth/reset-password", inviteToken, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void weakPasswordsAndUnknownTokensAreRejected() throws Exception {
        setPassword("/api/auth/activate", "anything", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        setPassword("/api/auth/activate", "unknown-token", NEW_PASSWORD)
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/auth/tokens/{token}", "unknown-token"))
                .andExpect(status().isBadRequest());
    }
}
