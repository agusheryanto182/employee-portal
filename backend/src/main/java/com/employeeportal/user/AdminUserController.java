package com.employeeportal.user;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.employeeportal.common.CurrentUser;
import com.employeeportal.common.PageResponse;
import com.employeeportal.common.Pagination;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Backoffice user management. {@code /api/admin/**} is restricted to ADMIN in {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Backoffice: users")
@SecurityRequirement(name = "bearer")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    @Operation(summary = "List users, searching email and full name, newest first")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size) {
        return userAdminService.list(search, status, role, Pagination.newestFirst(page, size));
    }

    @PostMapping
    @Operation(summary = "Register a user as PENDING")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userAdminService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + user.id())).body(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "User detail")
    public UserResponse get(@PathVariable UUID id) {
        return userAdminService.get(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit email, full name, phone or role; omitted fields are unchanged")
    public UserResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
        return userAdminService.update(id, request, CurrentUser.id(authentication));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Block sign-in and revoke all sessions")
    public UserResponse deactivate(@PathVariable UUID id, Authentication authentication) {
        return userAdminService.deactivate(id, CurrentUser.id(authentication));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Reactivate an INACTIVE user (back to PENDING if they never set a password)")
    public UserResponse activate(@PathVariable UUID id) {
        return userAdminService.activate(id);
    }
}
