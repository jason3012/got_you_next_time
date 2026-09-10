package com.settleup.settlement;

import com.settleup.common.Money;
import com.settleup.settlement.SettlementPlanner.Transfer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementPlannerTest {

    private final SettlementPlanner planner = new SettlementPlanner();
    private final UUID a = id(1);
    private final UUID b = id(2);
    private final UUID c = id(3);
    private final UUID d = id(4);

    @Test
    void returnsNoTransfersForSettledGroup() {
        assertThat(planner.plan(Map.of(a, Money.ZERO, b, Money.ZERO))).isEmpty();
    }

    @Test
    void settlesOneCreditorAndThreeDebtorsInThreeTransfers() {
        Map<UUID, Money> balances = Map.of(
                a, new Money(900),
                b, new Money(-300),
                c, new Money(-300),
                d, new Money(-300));

        List<Transfer> transfers = planner.plan(balances);

        assertThat(transfers).hasSize(3);
        assertSettlesExactly(balances, transfers);
    }

    @Test
    void settlesOneDebtorAndManyCreditors() {
        Map<UUID, Money> balances = Map.of(
                a, new Money(-750),
                b, new Money(100),
                c, new Money(250),
                d, new Money(400));

        List<Transfer> transfers = planner.plan(balances);

        assertThat(transfers).hasSize(3);
        assertSettlesExactly(balances, transfers);
    }

    @Test
    void excludesMembersWithZeroBalances() {
        Map<UUID, Money> balances = Map.of(
                a, new Money(-101),
                b, new Money(101),
                c, Money.ZERO,
                d, Money.ZERO);

        List<Transfer> transfers = planner.plan(balances);

        assertThat(transfers).hasSize(1);
        assertThat(transfers).allSatisfy(transfer -> {
            assertThat(transfer.fromUserId()).isNotIn(c, d);
            assertThat(transfer.toUserId()).isNotIn(c, d);
        });
        assertSettlesExactly(balances, transfers);
    }

    @Test
    void randomizedPlansSettleExactlyWithinTransferLimit() {
        Random random = new Random(8_142_024L);
        for (int attempt = 0; attempt < 500; attempt++) {
            int size = 2 + random.nextInt(15);
            Map<UUID, Money> balances = new HashMap<>();
            long total = 0;
            for (int index = 0; index < size - 1; index++) {
                long cents = random.nextInt(20_001) - 10_000;
                balances.put(id(index + 1), new Money(cents));
                total += cents;
            }
            balances.put(id(size), new Money(-total));

            List<Transfer> transfers = planner.plan(balances);

            assertThat(transfers.size()).isLessThanOrEqualTo(size - 1);
            assertSettlesExactly(balances, transfers);
        }
    }

    private void assertSettlesExactly(Map<UUID, Money> balances, List<Transfer> transfers) {
        Map<UUID, Long> remaining = new HashMap<>();
        balances.forEach((userId, balance) -> remaining.put(userId, balance.cents()));
        transfers.forEach(transfer -> {
            remaining.compute(transfer.fromUserId(), (id, value) -> value + transfer.amount().cents());
            remaining.compute(transfer.toUserId(), (id, value) -> value - transfer.amount().cents());
        });
        assertThat(remaining.values()).allMatch(value -> value == 0);
    }

    private UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
