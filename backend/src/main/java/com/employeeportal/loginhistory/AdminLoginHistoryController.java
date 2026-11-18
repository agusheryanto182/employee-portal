package com.employeeportal.loginhistory;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.employeeportal.common.PageResponse;
import com.employeeportal.common.Pagination;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/login-history")
@RequiredArgsConstructor
@Tag(name = "Backoffice: login history")
@SecurityRequirement(name = "bearer")
public class AdminLoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping
    @Operation(summary = "All login attempts, newest first",
            description = "`from` (inclusive) and `to` (exclusive) are ISO-8601 instants, for example 2026-11-18T00:00:00Z.")
    public PageResponse<LoginHistoryResponse> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size) {
        return loginHistoryService.search(
                new LoginHistoryFilter(userId, success, from, to), Pagination.newestFirst(page, size));
    }
}
