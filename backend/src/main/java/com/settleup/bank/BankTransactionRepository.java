package com.settleup.bank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findAllByAccountConnectionUserIdAndRemovedFalseOrderByPostedDateDescCreatedAtDesc(UUID userId);
    Optional<BankTransaction> findByIdAndAccountConnectionUserId(UUID id, UUID userId);
    Optional<BankTransaction> findByPlaidTransactionId(String plaidTransactionId);

    @Query("""
            select (count(transaction) > 0) from BankTransaction transaction
            where transaction.expense.group.id = :groupId
              and lower(coalesce(transaction.merchantName, transaction.name)) = lower(:merchant)
            """)
    boolean existsConfirmedMerchantForGroup(@Param("groupId") UUID groupId, @Param("merchant") String merchant);

    @Query("""
            select (count(transaction) > 0) from BankTransaction transaction
            where transaction.expense.group.id = :groupId
              and transaction.category = :category
            """)
    boolean existsConfirmedCategoryForGroup(@Param("groupId") UUID groupId, @Param("category") String category);
}
