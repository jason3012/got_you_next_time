package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.group.Group;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "expense_suggestions",
        uniqueConstraints = @UniqueConstraint(
                name = "expense_suggestions_transaction_group_unique",
                columnNames = {"transaction_id", "candidate_group_id"})
)
public class ExpenseSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private BankTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_group_id", nullable = false)
    private Group candidateGroup;

    @Column(nullable = false)
    private double confidence;

    @ElementCollection
    @CollectionTable(name = "expense_suggestion_reasons", joinColumns = @JoinColumn(name = "suggestion_id"))
    @OrderColumn(name = "reason_order")
    @Column(name = "reason", nullable = false, length = 500)
    private List<String> reasons = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseSuggestionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExpenseSuggestion() {
    }

    public ExpenseSuggestion(
            BankTransaction transaction,
            Group candidateGroup,
            double confidence,
            List<String> reasons
    ) {
        this.transaction = transaction;
        this.candidateGroup = candidateGroup;
        this.confidence = confidence;
        this.reasons.addAll(reasons);
        this.status = ExpenseSuggestionStatus.PENDING;
    }

    public void update(double confidence, List<String> reasons) {
        if (status != ExpenseSuggestionStatus.PENDING) {
            return;
        }
        this.confidence = confidence;
        this.reasons.clear();
        this.reasons.addAll(reasons);
    }

    public void confirm() {
        this.status = ExpenseSuggestionStatus.CONFIRMED;
    }

    public void reject() {
        this.status = ExpenseSuggestionStatus.REJECTED;
    }

    public UUID getId() { return id; }
    public BankTransaction getTransaction() { return transaction; }
    public Group getCandidateGroup() { return candidateGroup; }
    public double getConfidence() { return confidence; }
    public List<String> getReasons() { return List.copyOf(reasons); }
    public ExpenseSuggestionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
