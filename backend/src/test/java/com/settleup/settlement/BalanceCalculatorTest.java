package com.settleup.settlement;

import com.settleup.common.Money;
import com.settleup.settlement.BalanceCalculator.ExpenseEntry;
import com.settleup.settlement.BalanceCalculator.SettlementEntry;
import com.settleup.settlement.BalanceCalculator.Share;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceCalculatorTest {

    private final BalanceCalculator calculator = new BalanceCalculator();
    private final UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID casey = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void calculatesBalancesWithUnevenShares() {
        ExpenseEntry expense = new ExpenseEntry(alice, new Money(1_000), List.of(
                new Share(alice, new Money(334)),
                new Share(bob, new Money(333)),
                new Share(casey, new Money(333))));

        Map<UUID, Money> balances = calculator.calculate(
                List.of(alice, bob, casey), List.of(expense), List.of());

        assertThat(balances.get(alice).cents()).isEqualTo(666);
        assertThat(balances.get(bob).cents()).isEqualTo(-333);
        assertThat(balances.get(casey).cents()).isEqualTo(-333);
    }

    @Test
    void appliesPartialSettlementToFutureBalances() {
        ExpenseEntry expense = new ExpenseEntry(alice, new Money(1_000), List.of(
                new Share(alice, new Money(334)),
                new Share(bob, new Money(333)),
                new Share(casey, new Money(333))));
        SettlementEntry partialPayment = new SettlementEntry(bob, alice, new Money(100));

        Map<UUID, Money> balances = calculator.calculate(
                List.of(alice, bob, casey), List.of(expense), List.of(partialPayment));

        assertThat(balances.get(alice).cents()).isEqualTo(566);
        assertThat(balances.get(bob).cents()).isEqualTo(-233);
        assertThat(balances.get(casey).cents()).isEqualTo(-333);
    }

    @Test
    void rejectsExpenseWhoseSharesDoNotEqualItsAmount() {
        ExpenseEntry invalid = new ExpenseEntry(alice, new Money(1_000), List.of(
                new Share(alice, new Money(500)),
                new Share(bob, new Money(499))));

        assertThatThrownBy(() -> calculator.calculate(
                List.of(alice, bob), List.of(invalid), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("splits");
    }
}
