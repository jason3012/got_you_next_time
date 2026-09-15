package com.settleup.classification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface ExpenseSuggestionRepository extends JpaRepository<ExpenseSuggestion, UUID> {

    List<ExpenseSuggestion> findAllByTransactionAccountConnectionUserIdAndStatusOrderByConfidenceDescCreatedAtDesc(
            UUID userId,
            ExpenseSuggestionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpenseSuggestion> findByIdAndTransactionAccountConnectionUserId(UUID id, UUID userId);

    Optional<ExpenseSuggestion> findByTransactionIdAndCandidateGroupId(UUID transactionId, UUID candidateGroupId);

    List<ExpenseSuggestion> findAllByTransactionIdAndStatus(UUID transactionId, ExpenseSuggestionStatus status);

    void deleteAllByTransactionIdAndStatus(UUID transactionId, ExpenseSuggestionStatus status);

    @Query("""
            select (count(suggestion) > 0) from ExpenseSuggestion suggestion
            where suggestion.candidateGroup.id = :groupId
              and suggestion.status = com.settleup.classification.ExpenseSuggestionStatus.REJECTED
              and lower(coalesce(suggestion.transaction.merchantName, suggestion.transaction.name)) = lower(:merchant)
            """)
    boolean existsRejectedMerchantForGroup(@Param("groupId") UUID groupId, @Param("merchant") String merchant);
}
