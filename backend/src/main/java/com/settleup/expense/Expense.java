package com.settleup.expense;

import com.settleup.common.Money;
import com.settleup.group.Group;
import com.settleup.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Column(nullable = false)
    private String description;

    @Column(name = "amount_cents", nullable = false)
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_strategy", nullable = false)
    private SplitStrategy splitStrategy;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExpenseSplit> splits = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Expense() {
    }

    public Expense(Group group, User paidBy, String description, Money amount, SplitStrategy splitStrategy) {
        this.group = group;
        this.paidBy = paidBy;
        this.description = description;
        this.amount = amount;
        this.splitStrategy = splitStrategy;
    }

    public UUID getId() { return id; }
    public Group getGroup() { return group; }
    public User getPaidBy() { return paidBy; }
    public String getDescription() { return description; }
    public Money getAmount() { return amount; }
    public SplitStrategy getSplitStrategy() { return splitStrategy; }
    public Set<ExpenseSplit> getSplits() { return Set.copyOf(splits); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
