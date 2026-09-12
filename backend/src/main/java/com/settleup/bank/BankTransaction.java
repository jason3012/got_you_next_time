package com.settleup.bank;

import com.settleup.expense.Expense;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Column(name = "plaid_transaction_id", nullable = false, unique = true)
    private String plaidTransactionId;

    @Column(nullable = false)
    private String name;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "iso_currency_code", nullable = false)
    private String isoCurrencyCode;

    @Column(name = "authorized_date")
    private LocalDate authorizedDate;

    @Column(name = "posted_date", nullable = false)
    private LocalDate postedDate;

    @Column(nullable = false)
    private boolean pending;

    @Column(nullable = false)
    private boolean removed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BankTransaction() {
    }

    public BankTransaction(BankAccount account, PlaidClient.TransactionData data) {
        this.account = account;
        this.plaidTransactionId = data.transactionId();
        apply(data);
    }

    public void apply(PlaidClient.TransactionData data) {
        this.name = data.name();
        this.merchantName = data.merchantName();
        this.amountCents = data.amountCents();
        this.isoCurrencyCode = data.isoCurrencyCode();
        this.authorizedDate = data.authorizedDate();
        this.postedDate = data.postedDate();
        this.pending = data.pending();
        this.removed = false;
    }

    public void markRemoved() {
        this.removed = true;
    }

    public void linkExpense(Expense expense) {
        this.expense = expense;
    }

    public UUID getId() { return id; }
    public BankAccount getAccount() { return account; }
    public String getPlaidTransactionId() { return plaidTransactionId; }
    public String getName() { return name; }
    public String getMerchantName() { return merchantName; }
    public long getAmountCents() { return amountCents; }
    public String getIsoCurrencyCode() { return isoCurrencyCode; }
    public LocalDate getAuthorizedDate() { return authorizedDate; }
    public LocalDate getPostedDate() { return postedDate; }
    public boolean isPending() { return pending; }
    public boolean isRemoved() { return removed; }
    public Expense getExpense() { return expense; }
}
