package com.settleup.bank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankConnectionRepository extends JpaRepository<BankConnection, UUID> {
    List<BankConnection> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<BankConnection> findByIdAndUserId(UUID id, UUID userId);
    Optional<BankConnection> findByPlaidItemId(String plaidItemId);
    boolean existsByPlaidItemId(String plaidItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select connection from BankConnection connection where connection.id = :id and connection.user.id = :userId")
    Optional<BankConnection> findLockedByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select connection from BankConnection connection where connection.plaidItemId = :itemId")
    Optional<BankConnection> findLockedByPlaidItemId(@Param("itemId") String itemId);
}
