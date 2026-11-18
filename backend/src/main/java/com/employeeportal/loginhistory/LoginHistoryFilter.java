package com.employeeportal.loginhistory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

/**
 * Optional filters for the backoffice login history. {@code from} is inclusive and {@code to} is
 * exclusive, so consecutive ranges never count an attempt twice.
 */
public record LoginHistoryFilter(UUID userId, Boolean success, Instant from, Instant to) {

    Specification<LoginHistory> toSpecification() {
        List<Specification<LoginHistory>> filters = new ArrayList<>();
        if (userId != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (success != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("success"), success));
        }
        if (from != null) {
            filters.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            filters.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
        }
        return Specification.allOf(filters);
    }
}
