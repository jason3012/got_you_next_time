package com.settleup.bank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    List<BankAccount> findAllByConnectionIdOrderByNameAsc(UUID connectionId);
    Optional<BankAccount> findByPlaidAccountId(String plaidAccountId);
}
