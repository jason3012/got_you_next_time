package com.settleup.bank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findAllByAccountConnectionUserIdAndRemovedFalseOrderByPostedDateDescCreatedAtDesc(UUID userId);
    Optional<BankTransaction> findByIdAndAccountConnectionUserId(UUID id, UUID userId);
    Optional<BankTransaction> findByPlaidTransactionId(String plaidTransactionId);
}
