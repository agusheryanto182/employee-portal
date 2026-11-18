package com.employeeportal.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Shared paging rules: 0-based page, size 1 to {@value #MAX_SIZE} (validated in the controllers). */
public final class Pagination {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    /** Newest first, with the id as a tie-breaker so pages never overlap or skip rows. */
    public static Pageable newestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
}
