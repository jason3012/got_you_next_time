package com.settleup.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findAllByGroupIdOrderByCreatedAtDesc(UUID groupId);
    Optional<Expense> findByIdAndGroupId(UUID id, UUID groupId);
    boolean existsByGroupIdAndPaidById(UUID groupId, UUID userId);
    boolean existsByGroupIdAndCreatedById(UUID groupId, UUID userId);
}
