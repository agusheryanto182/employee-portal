package com.employeeportal.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/** Builds the WHERE clause for the backoffice user list from the optional filters. */
final class UserSpecifications {

    private UserSpecifications() {
    }

    static Specification<User> matching(String search, UserStatus status, Role role) {
        List<Specification<User>> filters = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
            filters.add((root, query, cb) -> cb.or(
                    cb.like(root.get("email"), pattern, '\\'),
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\')));
        }
        if (status != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (role != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        return Specification.allOf(filters);
    }

    /** Searching for "50%" must match the text "50%", not "50" followed by anything. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
