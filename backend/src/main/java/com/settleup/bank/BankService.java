package com.settleup.bank;

import com.settleup.bank.dto.BankAccountResponse;
import com.settleup.bank.dto.BankConnectionResponse;
import com.settleup.bank.dto.BankTransactionResponse;
import com.settleup.bank.dto.ExchangePublicTokenRequest;
import com.settleup.bank.dto.ImportTransactionRequest;
import com.settleup.bank.dto.LinkTokenResponse;
import com.settleup.bank.dto.SyncResponse;
import com.settleup.common.exception.ConflictException;
import com.settleup.common.exception.NotFoundException;
import com.settleup.common.exception.ValidationException;
import com.settleup.expense.Expense;
import com.settleup.expense.ExpenseRepository;
import com.settleup.expense.ExpenseService;
import com.settleup.expense.dto.CreateExpenseRequest;
import com.settleup.expense.dto.ExpenseResponse;
import com.settleup.user.User;
import com.settleup.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BankService {

    private static final String PAGINATION_MUTATION = "TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION";
    private static final int MAX_SYNC_ATTEMPTS = 3;

    private final PlaidClient plaidClient;
    private final TokenCipher tokenCipher;
    private final BankConnectionRepository connectionRepository;
    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;

    @Autowired
    public BankService(
            PlaidClient plaidClient,
            TokenCipher tokenCipher,
            BankConnectionRepository connectionRepository,
            BankAccountRepository accountRepository,
            BankTransactionRepository transactionRepository,
            UserRepository userRepository,
            ExpenseService expenseService,
            ExpenseRepository expenseRepository
    ) {
        this(plaidClient, tokenCipher, connectionRepository, accountRepository, transactionRepository,
                userRepository, expenseService, expenseRepository, Clock.systemUTC());
    }

    BankService(
            PlaidClient plaidClient,
            TokenCipher tokenCipher,
            BankConnectionRepository connectionRepository,
            BankAccountRepository accountRepository,
            BankTransactionRepository transactionRepository,
            UserRepository userRepository,
            ExpenseService expenseService,
            ExpenseRepository expenseRepository,
            Clock clock
    ) {
        this.plaidClient = plaidClient;
        this.tokenCipher = tokenCipher;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
    }

    public LinkTokenResponse createLinkToken(UUID userId) {
        requireUser(userId);
        PlaidClient.LinkToken token = plaidClient.createLinkToken(userId.toString());
        return new LinkTokenResponse(token.value(), token.expiration());
    }

    @Transactional
    public BankConnectionResponse exchange(UUID userId, ExchangePublicTokenRequest request) {
        User user = requireUser(userId);
        PlaidClient.ExchangedItem item = plaidClient.exchangePublicToken(request.publicToken());
        if (connectionRepository.existsByPlaidItemId(item.itemId())) {
            throw new ConflictException("This bank connection already exists");
        }

        BankConnection connection = connectionRepository.saveAndFlush(new BankConnection(
                user,
                item.itemId(),
                blankToNull(request.institutionId()),
                defaultInstitutionName(request.institutionName()),
                tokenCipher.encrypt(item.accessToken())));
        upsertAccounts(connection, plaidClient.getAccounts(item.accessToken()));
        try {
            synchronize(connection);
        } catch (PlaidApiException exception) {
            connection.failed(exception.getErrorCode());
        }
        return toConnectionResponse(connection);
    }

    @Transactional(readOnly = true)
    public List<BankConnectionResponse> listConnections(UUID userId) {
        requireUser(userId);
        return connectionRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toConnectionResponse)
                .toList();
    }

    @Transactional
    public SyncResponse sync(UUID userId, UUID connectionId) {
        BankConnection connection = connectionRepository.findLockedByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new NotFoundException("Bank connection was not found"));
        return synchronize(connection);
    }

    @Transactional
    public void syncWebhook(String itemId) {
        BankConnection connection = connectionRepository.findLockedByPlaidItemId(itemId)
                .orElse(null);
        if (connection == null) {
            return;
        }
        try {
            synchronize(connection);
        } catch (PlaidApiException exception) {
            connection.failed(exception.getErrorCode());
        }
    }

    @Transactional
    public void disconnect(UUID userId, UUID connectionId) {
        BankConnection connection = connectionRepository.findLockedByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new NotFoundException("Bank connection was not found"));
        plaidClient.removeItem(tokenCipher.decrypt(connection.getEncryptedAccessToken()));
        connectionRepository.delete(connection);
    }

    @Transactional(readOnly = true)
    public List<BankTransactionResponse> listTransactions(UUID userId) {
        requireUser(userId);
        return transactionRepository.findAllByAccountConnectionUserIdAndRemovedFalseOrderByPostedDateDescCreatedAtDesc(userId)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional
    public ExpenseResponse importTransaction(UUID userId, UUID transactionId, ImportTransactionRequest request) {
        BankTransaction transaction = transactionRepository.findByIdAndAccountConnectionUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Bank transaction was not found"));
        if (transaction.isRemoved() || transaction.isPending()) {
            throw new ValidationException("Removed or pending transactions cannot be imported");
        }
        if (transaction.getExpense() != null) {
            throw new ConflictException("This transaction has already been imported");
        }
        if (transaction.getAmountCents() <= 0) {
            throw new ValidationException("Only outgoing transactions can be imported as expenses");
        }

        String description = blankToNull(request.description());
        ExpenseResponse response = expenseService.create(
                request.groupId(),
                userId,
                new CreateExpenseRequest(
                        description == null ? transactionDisplayName(transaction) : description,
                        transaction.getAmountCents(),
                        userId,
                        request.splitStrategy(),
                        request.splits()));
        Expense expense = expenseRepository.getReferenceById(response.id());
        transaction.linkExpense(expense);
        return response;
    }

    private SyncResponse synchronize(BankConnection connection) {
        String accessToken = tokenCipher.decrypt(connection.getEncryptedAccessToken());
        String startingCursor = connection.getSyncCursor();
        for (int attempt = 1; attempt <= MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                return synchronizeFromCursor(connection, accessToken, startingCursor);
            } catch (PlaidApiException exception) {
                if (!PAGINATION_MUTATION.equals(exception.getErrorCode()) || attempt == MAX_SYNC_ATTEMPTS) {
                    connection.failed(exception.getErrorCode());
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("Transaction sync retry loop ended unexpectedly");
    }

    private SyncResponse synchronizeFromCursor(
            BankConnection connection,
            String accessToken,
            String startingCursor
    ) {
        Map<String, PlaidClient.TransactionData> added = new LinkedHashMap<>();
        Map<String, PlaidClient.TransactionData> modified = new LinkedHashMap<>();
        Map<String, Boolean> removed = new LinkedHashMap<>();
        String cursor = startingCursor;
        boolean hasMore;
        do {
            PlaidClient.SyncPage page = plaidClient.syncTransactions(accessToken, cursor);
            page.added().forEach(transaction -> added.put(transaction.transactionId(), transaction));
            page.modified().forEach(transaction -> modified.put(transaction.transactionId(), transaction));
            page.removedTransactionIds().forEach(id -> removed.put(id, true));
            cursor = page.nextCursor();
            hasMore = page.hasMore();
        } while (hasMore);

        Map<String, BankAccount> accounts = accountRepository.findAllByConnectionIdOrderByNameAsc(connection.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(BankAccount::getPlaidAccountId, account -> account));
        added.values().forEach(transaction -> upsertTransaction(accounts, transaction));
        modified.values().forEach(transaction -> upsertTransaction(accounts, transaction));
        removed.keySet().forEach(id -> transactionRepository.findByPlaidTransactionId(id)
                .ifPresent(BankTransaction::markRemoved));
        Instant syncedAt = clock.instant();
        connection.synced(cursor, syncedAt);
        return new SyncResponse(added.size(), modified.size(), removed.size(), syncedAt);
    }

    private void upsertAccounts(BankConnection connection, List<PlaidClient.AccountData> accounts) {
        for (PlaidClient.AccountData data : accounts) {
            BankAccount account = accountRepository.findByPlaidAccountId(data.accountId())
                    .orElseGet(() -> new BankAccount(
                            connection,
                            data.accountId(),
                            data.name(),
                            data.officialName(),
                            data.mask(),
                            data.type(),
                            data.subtype()));
            account.update(data.name(), data.officialName(), data.mask(), data.type(), data.subtype());
            accountRepository.save(account);
        }
        accountRepository.flush();
    }

    private void upsertTransaction(Map<String, BankAccount> accounts, PlaidClient.TransactionData data) {
        BankAccount account = accounts.get(data.accountId());
        if (account == null) {
            throw new PlaidApiException(
                    "UNKNOWN_ACCOUNT",
                    "Plaid returned a transaction for an unknown account",
                    null);
        }
        BankTransaction transaction = transactionRepository.findByPlaidTransactionId(data.transactionId())
                .orElseGet(() -> new BankTransaction(account, data));
        transaction.apply(data);
        transactionRepository.save(transaction);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
    }

    private BankConnectionResponse toConnectionResponse(BankConnection connection) {
        List<BankAccountResponse> accounts = accountRepository
                .findAllByConnectionIdOrderByNameAsc(connection.getId()).stream()
                .sorted(Comparator.comparing(BankAccount::getName))
                .map(account -> new BankAccountResponse(
                        account.getId(),
                        account.getName(),
                        account.getOfficialName(),
                        account.getMask(),
                        account.getAccountType(),
                        account.getSubtype()))
                .toList();
        return new BankConnectionResponse(
                connection.getId(),
                connection.getInstitutionName(),
                connection.getStatus(),
                connection.getErrorCode(),
                connection.getLastSyncedAt(),
                connection.getCreatedAt(),
                accounts);
    }

    private BankTransactionResponse toTransactionResponse(BankTransaction transaction) {
        return new BankTransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getName(),
                transaction.getMerchantName(),
                transaction.getAmountCents(),
                transaction.getIsoCurrencyCode(),
                transaction.getAuthorizedDate(),
                transaction.getPostedDate(),
                transaction.isPending(),
                transaction.getExpense() == null ? null : transaction.getExpense().getId());
    }

    private String transactionDisplayName(BankTransaction transaction) {
        return transaction.getMerchantName() == null ? transaction.getName() : transaction.getMerchantName();
    }

    private String defaultInstitutionName(String institutionName) {
        String value = blankToNull(institutionName);
        return value == null ? "Connected institution" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
