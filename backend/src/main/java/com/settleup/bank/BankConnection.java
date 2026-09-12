package com.settleup.bank;

import com.settleup.user.User;
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
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_connections")
public class BankConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plaid_item_id", nullable = false, unique = true)
    private String plaidItemId;

    @Column(name = "institution_id")
    private String institutionId;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "encrypted_access_token", nullable = false)
    private String encryptedAccessToken;

    @Column(name = "sync_cursor")
    private String syncCursor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankConnectionStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BankConnection() {
    }

    public BankConnection(
            User user,
            String plaidItemId,
            String institutionId,
            String institutionName,
            String encryptedAccessToken
    ) {
        this.user = user;
        this.plaidItemId = plaidItemId;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.encryptedAccessToken = encryptedAccessToken;
        this.status = BankConnectionStatus.HEALTHY;
    }

    public void synced(String cursor, Instant at) {
        this.syncCursor = cursor;
        this.lastSyncedAt = at;
        this.status = BankConnectionStatus.HEALTHY;
        this.errorCode = null;
    }

    public void failed(String errorCode) {
        this.status = BankConnectionStatus.ERROR;
        this.errorCode = errorCode;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getPlaidItemId() { return plaidItemId; }
    public String getInstitutionId() { return institutionId; }
    public String getInstitutionName() { return institutionName; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public String getSyncCursor() { return syncCursor; }
    public BankConnectionStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
