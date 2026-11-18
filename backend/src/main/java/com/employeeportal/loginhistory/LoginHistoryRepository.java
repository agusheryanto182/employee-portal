package com.employeeportal.loginhistory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID>, JpaSpecificationExecutor<LoginHistory> {

    List<LoginHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<LoginHistory> findByEmailAttemptedOrderByCreatedAtDesc(String emailAttempted);
}
