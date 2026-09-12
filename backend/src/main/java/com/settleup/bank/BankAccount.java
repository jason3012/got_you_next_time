package com.settleup.bank;

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
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private BankConnection connection;

    @Column(name = "plaid_account_id", nullable = false, unique = true)
    private String plaidAccountId;

    @Column(nullable = false)
    private String name;

    @Column(name = "official_name")
    private String officialName;

    private String mask;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    private String subtype;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BankAccount() {
    }

    public BankAccount(
            BankConnection connection,
            String plaidAccountId,
            String name,
            String officialName,
            String mask,
            String accountType,
            String subtype
    ) {
        this.connection = connection;
        this.plaidAccountId = plaidAccountId;
        this.name = name;
        this.officialName = officialName;
        this.mask = mask;
        this.accountType = accountType;
        this.subtype = subtype;
    }

    public void update(String name, String officialName, String mask, String accountType, String subtype) {
        this.name = name;
        this.officialName = officialName;
        this.mask = mask;
        this.accountType = accountType;
        this.subtype = subtype;
    }

    public UUID getId() { return id; }
    public BankConnection getConnection() { return connection; }
    public String getPlaidAccountId() { return plaidAccountId; }
    public String getName() { return name; }
    public String getOfficialName() { return officialName; }
    public String getMask() { return mask; }
    public String getAccountType() { return accountType; }
    public String getSubtype() { return subtype; }
}
