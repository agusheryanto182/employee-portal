package com.employeeportal.loginhistory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    List<LoginHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<LoginHistory> findByEmailAttemptedOrderByCreatedAtDesc(String emailAttempted);
}
