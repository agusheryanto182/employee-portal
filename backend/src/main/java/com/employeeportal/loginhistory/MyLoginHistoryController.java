package com.employeeportal.loginhistory;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.employeeportal.common.CurrentUser;
import com.employeeportal.common.PageResponse;
import com.employeeportal.common.Pagination;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/login-history")
@RequiredArgsConstructor
@Tag(name = "My account")
@SecurityRequirement(name = "bearer")
public class MyLoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping
    @Operation(summary = "My login attempts, successful and failed, newest first")
    public PageResponse<LoginHistoryResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size) {
        return loginHistoryService.search(
                new LoginHistoryFilter(CurrentUser.id(authentication), null, null, null), Pagination.newestFirst(page, size));
    }
}
