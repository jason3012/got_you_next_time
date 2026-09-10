package com.settleup.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findAllByGroupIdOrderByCreatedAtAsc(UUID groupId);
    boolean existsByGroupIdAndFromUserId(UUID groupId, UUID userId);
    boolean existsByGroupIdAndToUserId(UUID groupId, UUID userId);
}
