package com.settleup.settlement;

import com.settleup.common.Money;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

public class SettlementPlanner {

    private static final Comparator<Account> LARGEST_FIRST = Comparator
            .comparingLong(Account::amountCents)
            .reversed()
            .thenComparing(Account::userId);

    public List<Transfer> plan(Map<UUID, Money> balances) {
        long total = balances.values().stream()
                .mapToLong(Money::cents)
                .reduce(0, Math::addExact);
        if (total != 0) {
            throw new IllegalArgumentException("Balances must sum to zero");
        }

        PriorityQueue<Account> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Account> debtors = new PriorityQueue<>(LARGEST_FIRST);
        balances.forEach((userId, balance) -> {
            if (balance.cents() > 0) {
                creditors.add(new Account(userId, balance.cents()));
            } else if (balance.cents() < 0) {
                debtors.add(new Account(userId, Math.negateExact(balance.cents())));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Account debtor = debtors.remove();
            Account creditor = creditors.remove();
            long amount = Math.min(debtor.amountCents(), creditor.amountCents());
            transfers.add(new Transfer(debtor.userId(), creditor.userId(), new Money(amount)));

            long debtRemaining = debtor.amountCents() - amount;
            long creditRemaining = creditor.amountCents() - amount;
            if (debtRemaining > 0) {
                debtors.add(new Account(debtor.userId(), debtRemaining));
            }
            if (creditRemaining > 0) {
                creditors.add(new Account(creditor.userId(), creditRemaining));
            }
        }

        if (!debtors.isEmpty() || !creditors.isEmpty()) {
            throw new IllegalStateException("Settlement plan did not reach zero");
        }
        return List.copyOf(transfers);
    }

    private record Account(UUID userId, long amountCents) {
    }

    public record Transfer(UUID fromUserId, UUID toUserId, Money amount) {
    }
}
