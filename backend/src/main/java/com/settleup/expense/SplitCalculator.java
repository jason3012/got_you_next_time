package com.settleup.expense;

import com.settleup.common.Money;
import com.settleup.common.exception.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SplitCalculator {

    public List<CalculatedSplit> calculate(
            Money total,
            SplitStrategy strategy,
            List<SplitInput> inputs
    ) {
        if (total == null || total.cents() <= 0) {
            throw new ValidationException("Expense amount must be greater than zero");
        }
        if (strategy == null) {
            throw new ValidationException("Split strategy is required");
        }
        validateParticipants(inputs);

        return switch (strategy) {
            case EQUAL -> equal(total, inputs);
            case EXACT -> exact(total, inputs);
            case PERCENTAGE -> percentage(total, inputs);
        };
    }

    private List<CalculatedSplit> equal(Money total, List<SplitInput> inputs) {
        long base = total.cents() / inputs.size();
        long remainder = total.cents() % inputs.size();
        List<CalculatedSplit> result = new ArrayList<>(inputs.size());
        for (int index = 0; index < inputs.size(); index++) {
            long share = base + (index < remainder ? 1 : 0);
            result.add(new CalculatedSplit(inputs.get(index).userId(), new Money(share)));
        }
        return List.copyOf(result);
    }

    private List<CalculatedSplit> exact(Money total, List<SplitInput> inputs) {
        List<CalculatedSplit> result = new ArrayList<>(inputs.size());
        long sum = 0;
        try {
            for (SplitInput input : inputs) {
                if (input.amount() == null || input.amount().cents() < 0) {
                    throw new ValidationException("Every exact split needs a non-negative amount");
                }
                sum = Math.addExact(sum, input.amount().cents());
                result.add(new CalculatedSplit(input.userId(), input.amount()));
            }
        } catch (ArithmeticException exception) {
            throw new ValidationException("Exact split total is too large");
        }
        if (sum != total.cents()) {
            throw new ValidationException("Exact splits must equal the expense amount");
        }
        return List.copyOf(result);
    }

    private List<CalculatedSplit> percentage(Money total, List<SplitInput> inputs) {
        int percentageTotal = inputs.stream()
                .map(SplitInput::percentage)
                .peek(value -> {
                    if (value == null || value < 0 || value > 100) {
                        throw new ValidationException("Every percentage must be between 0 and 100");
                    }
                })
                .mapToInt(Integer::intValue)
                .sum();
        if (percentageTotal != 100) {
            throw new ValidationException("Percentage splits must total exactly 100");
        }

        List<Long> shares = new ArrayList<>(inputs.size());
        long allocated = 0;
        try {
            for (SplitInput input : inputs) {
                long share = Math.multiplyExact(total.cents(), input.percentage()) / 100;
                shares.add(share);
                allocated = Math.addExact(allocated, share);
            }
        } catch (ArithmeticException exception) {
            throw new ValidationException("Percentage split amount is too large");
        }

        long remainder = total.cents() - allocated;
        List<CalculatedSplit> result = new ArrayList<>(inputs.size());
        for (int index = 0; index < inputs.size(); index++) {
            long share = shares.get(index) + (index < remainder ? 1 : 0);
            result.add(new CalculatedSplit(inputs.get(index).userId(), new Money(share)));
        }
        return List.copyOf(result);
    }

    private void validateParticipants(List<SplitInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new ValidationException("At least one split participant is required");
        }
        Set<UUID> participantIds = new HashSet<>();
        for (SplitInput input : inputs) {
            if (input == null || input.userId() == null) {
                throw new ValidationException("Every split participant needs a user ID");
            }
            if (!participantIds.add(input.userId())) {
                throw new ValidationException("Split participants must be unique");
            }
        }
    }

    public record SplitInput(UUID userId, Money amount, Integer percentage) {
    }

    public record CalculatedSplit(UUID userId, Money amount) {
    }
}
