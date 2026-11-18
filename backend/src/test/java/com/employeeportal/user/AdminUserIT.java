package com.employeeportal.user;

import static com.employeeportal.support.TestAccounts.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Backoffice user management over HTTP against a real database. */
@IntegrationTest
class AdminUserIT {

    private static final Pattern REFRESH_COOKIE = Pattern.compile("refresh_token=([^;]*)");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestAccounts accounts;
    @Autowired
    UserRepository userRepository;

    User admin;
    String adminAuth;

    @BeforeEach
    void signInAsAdmin() throws Exception {
        admin = accounts.active(Role.ADMIN);
        adminAuth = accounts.bearer(admin);
    }

    private ResultActions createUser(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions patchUser(UUID id, String json) throws Exception {
        return mockMvc.perform(patch("/api/admin/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions action(UUID id, String action) throws Exception {
        return mockMvc.perform(post("/api/admin/users/{id}/{action}", id, action)
                .header(HttpHeaders.AUTHORIZATION, adminAuth));
    }

    private int refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refreshToken)))
                .andReturn().getResponse().getStatus();
    }

    private static String refreshToken(MvcResult login) {
        Matcher matcher = REFRESH_COOKIE.matcher(login.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    // --- Create ------------------------------------------------------------------------------

    @Test
    void createsAPendingUserWithANormalizedEmail() throws Exception {
        String email = uniqueEmail("new");

        MvcResult result = createUser("""
                {"email": "%s", "fullName": " Budi Santoso ", "phone": "+62 812-3456-7890", "role": "EMPLOYEE"}
                """.formatted(email.toUpperCase()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/admin/users/")))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Budi Santoso"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        User saved = userRepository.findById(id).orElseThrow();
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegisteredInAnyCase() throws Exception {
        User existing = accounts.active(Role.EMPLOYEE);

        createUser("""
                {"email": "%s", "fullName": "Someone", "role": "EMPLOYEE"}
                """.formatted(existing.getEmail().toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already registered"));
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        createUser("""
                {"email": "not-an-email", "fullName": "", "phone": "abc"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    // --- List and detail ---------------------------------------------------------------------

    @Test
    void listSearchesNameAndEmailFiltersAndPaginates() throws Exception {
        String tag = "zz" + UUID.randomUUID().toString().substring(0, 8);
        accounts.active(Role.EMPLOYEE, "Alpha " + tag);
        accounts.active(Role.EMPLOYEE, "Beta " + tag);
        User inactive = accounts.active(Role.EMPLOYEE, "Gamma " + tag);
        action(inactive.getId(), "deactivate").andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("search", tag.toUpperCase()).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].fullName").value("Gamma " + tag)); // newest first

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("search", tag).param("status", "INACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("INACTIVE"))));
    }

    @Test
    void searchTreatsLikeWildcardsAsText() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .param("search", "%_" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pageSizeIsLimited() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, adminAuth).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User not found"));
    }

    // --- Update ------------------------------------------------------------------------------

    @Test
    void patchChangesOnlyTheGivenFields() throws Exception {
        User user = accounts.active(Role.EMPLOYEE, "Old Name");
        String newEmail = uniqueEmail("renamed");

        patchUser(user.getId(), """
                {"email": "%s", "phone": "0812345678"}
                """.formatted(newEmail.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.phone").value("0812345678"))
                .andExpect(jsonPath("$.fullName").value("Old Name"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        patchUser(user.getId(), """
                {"phone": ""}
                """)
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void patchRejectsAnEmailOfAnotherUserAndBlankNames() throws Exception {
        User user = accounts.active(Role.EMPLOYEE);
        User other = accounts.active(Role.EMPLOYEE);

        patchUser(user.getId(), """
                {"email": "%s"}
                """.formatted(other.getEmail()))
                .andExpect(status().isConflict());

        patchUser(user.getId(), """
                {"fullName": "   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void changingTheRoleSignsTheUserOutEverywhere() throws Exception {
        User user = accounts.active(Role.EMPLOYEE);
        String refreshToken = refreshToken(accounts.signIn(user));

        patchUser(user.getId(), """
                {"role": "ADMIN"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertThat(refresh(refreshToken)).isEqualTo(401);
    }

    // --- Deactivate and activate -------------------------------------------------------------

    @Test
    void deactivationBlocksSignInAndRevokesSessionsUntilReactivated() throws Exception {
        User user = accounts.active(Role.EMPLOYEE);
        String refreshToken = refreshToken(accounts.signIn(user));

        action(user.getId(), "deactivate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        assertThat(refresh(refreshToken)).isEqualTo(401);
        assertThat(accounts.signIn(user).getResponse().getStatus()).isEqualTo(401);

        action(user.getId(), "activate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(accounts.signIn(user).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void aPendingUserCanBeDeactivatedAndReactivatesAsPending() throws Exception {
        String id = JsonPath.read(createUser("""
                {"email": "%s", "fullName": "Invited", "role": "EMPLOYEE"}
                """.formatted(uniqueEmail("pending"))).andReturn().getResponse().getContentAsString(), "$.id");

        action(UUID.fromString(id), "deactivate").andExpect(jsonPath("$.status").value("INACTIVE"));
        action(UUID.fromString(id), "activate").andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void anAdminCannotDeactivateThemselvesOrChangeTheirOwnRole() throws Exception {
        action(admin.getId(), "deactivate")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("You cannot deactivate your own account"));

        patchUser(admin.getId(), """
                {"role": "EMPLOYEE"}
                """)
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(admin.getId()).orElseThrow())
                .satisfies(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
                });
    }

    // --- Access ------------------------------------------------------------------------------

    @Test
    void onlyAdminsCanManageUsers() throws Exception {
        String employeeAuth = accounts.bearer(accounts.active(Role.EMPLOYEE));

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, employeeAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
