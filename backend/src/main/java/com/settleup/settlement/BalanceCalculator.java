package com.settleup.settlement;

import com.settleup.common.Money;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BalanceCalculator {

    public Map<UUID, Money> calculate(
            Collection<UUID> memberIds,
            Collection<ExpenseEntry> expenses,
            Collection<SettlementEntry> settlements
    ) {
        Map<UUID, Money> balances = new LinkedHashMap<>();
        memberIds.stream().sorted(Comparator.naturalOrder()).forEach(id -> balances.put(id, Money.ZERO));

        for (ExpenseEntry expense : expenses) {
            requireMember(balances, expense.payerId());
            long splitTotal = 0;
            for (Share share : expense.shares()) {
                requireMember(balances, share.userId());
                splitTotal = Math.addExact(splitTotal, share.amount().cents());
                balances.compute(share.userId(), (id, balance) -> balance.minus(share.amount()));
            }
            if (splitTotal != expense.amount().cents()) {
                throw new IllegalStateException("Expense splits do not equal the expense total");
            }
            balances.compute(expense.payerId(), (id, balance) -> balance.plus(expense.amount()));
        }

        for (SettlementEntry settlement : settlements) {
            requireMember(balances, settlement.fromUserId());
            requireMember(balances, settlement.toUserId());
            balances.compute(settlement.fromUserId(), (id, balance) -> balance.plus(settlement.amount()));
            balances.compute(settlement.toUserId(), (id, balance) -> balance.minus(settlement.amount()));
        }

        long total = balances.values().stream()
                .mapToLong(Money::cents)
                .reduce(0, Math::addExact);
        if (total != 0) {
            throw new IllegalStateException("Group balances must sum to zero");
        }
        return Map.copyOf(balances);
    }

    private void requireMember(Map<UUID, Money> balances, UUID userId) {
        if (!balances.containsKey(userId)) {
            throw new IllegalStateException("Balance entry references a non-member");
        }
    }

    public record ExpenseEntry(UUID payerId, Money amount, List<Share> shares) {
    }

    public record Share(UUID userId, Money amount) {
    }

    public record SettlementEntry(UUID fromUserId, UUID toUserId, Money amount) {
    }
}
