package com.settleup.expense;

import com.settleup.common.Money;
import com.settleup.common.exception.ValidationException;
import com.settleup.expense.SplitCalculator.CalculatedSplit;
import com.settleup.expense.SplitCalculator.SplitInput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitCalculatorTest {

    private final SplitCalculator calculator = new SplitCalculator();
    private final UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID third = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void splitsEvenly() {
        List<CalculatedSplit> result = calculator.calculate(
                new Money(1_000), SplitStrategy.EQUAL, inputs(first, second));

        assertThat(cents(result)).containsExactly(500L, 500L);
    }

    @Test
    void distributesEqualRemainderInInputOrder() {
        List<CalculatedSplit> result = calculator.calculate(
                new Money(1_000), SplitStrategy.EQUAL, inputs(first, second, third));

        assertThat(cents(result)).containsExactly(334L, 333L, 333L);
        assertThat(sum(result)).isEqualTo(1_000);
    }

    @Test
    void rejectsExactSplitsThatDoNotMatchTotal() {
        List<SplitInput> inputs = List.of(
                new SplitInput(first, new Money(400), null),
                new SplitInput(second, new Money(500), null));

        assertThatThrownBy(() -> calculator.calculate(new Money(1_000), SplitStrategy.EXACT, inputs))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("equal");
    }

    @Test
    void roundsPercentageSplitsWithoutLosingCents() {
        List<SplitInput> inputs = List.of(
                new SplitInput(first, null, 50),
                new SplitInput(second, null, 50));

        List<CalculatedSplit> result = calculator.calculate(
                new Money(101), SplitStrategy.PERCENTAGE, inputs);

        assertThat(cents(result)).containsExactly(51L, 50L);
        assertThat(sum(result)).isEqualTo(101);
    }

    @Test
    void supportsSingleMemberGroup() {
        List<CalculatedSplit> result = calculator.calculate(
                new Money(875), SplitStrategy.EQUAL, inputs(first));

        assertThat(cents(result)).containsExactly(875L);
    }

    private List<SplitInput> inputs(UUID... userIds) {
        return java.util.Arrays.stream(userIds)
                .map(userId -> new SplitInput(userId, null, null))
                .toList();
    }

    private List<Long> cents(List<CalculatedSplit> splits) {
        return splits.stream().map(split -> split.amount().cents()).toList();
    }

    private long sum(List<CalculatedSplit> splits) {
        return splits.stream().mapToLong(split -> split.amount().cents()).sum();
    }
}
