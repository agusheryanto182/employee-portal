package com.employeeportal.loginhistory;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.employeeportal.support.IntegrationTest;
import com.employeeportal.support.TestAccounts;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;

/** Backoffice view of every login attempt. */
@IntegrationTest
class AdminLoginHistoryIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestAccounts accounts;

    private void failLogin(User user) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "wrong-password"}
                        """.formatted(user.getEmail())));
    }

    @Test
    void filtersByUserAndOutcomeNewestFirst() throws Exception {
        String adminAuth = accounts.bearer(accounts.active(Role.ADMIN));
        User employee = accounts.active(Role.EMPLOYEE);
        failLogin(employee);
        accounts.signIn(employee);

        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("userId", employee.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].success").value(true))
                .andExpect(jsonPath("$.content[1].success").value(false))
                .andExpect(jsonPath("$.content[1].failureReason").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.content[1].email").value(employee.getEmail()));

        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("userId", employee.getId().toString()).param("success", "false"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].success", everyItem(is(false))));
    }

    @Test
    void filtersByDateRange() throws Exception {
        String adminAuth = accounts.bearer(accounts.active(Role.ADMIN));
        User employee = accounts.active(Role.EMPLOYEE);
        accounts.signIn(employee);
        Instant now = Instant.now();

        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("userId", employee.getId().toString())
                        .param("from", now.minus(1, ChronoUnit.HOURS).toString())
                        .param("to", now.plus(1, ChronoUnit.HOURS).toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("userId", employee.getId().toString())
                        .param("to", now.minus(1, ChronoUnit.HOURS).toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidFiltersAreRejected() throws Exception {
        String adminAuth = accounts.bearer(accounts.active(Role.ADMIN));

        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("from", "yesterday"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeesCannotSeeEveryonesHistory() throws Exception {
        String employeeAuth = accounts.bearer(accounts.active(Role.EMPLOYEE));

        mockMvc.perform(get("/api/admin/login-history").header(HttpHeaders.AUTHORIZATION, employeeAuth))
                .andExpect(status().isForbidden());
    }
}
