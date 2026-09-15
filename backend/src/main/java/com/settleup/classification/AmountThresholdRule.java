package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.expense.Expense;
import com.settleup.expense.ExpenseRepository;
import com.settleup.group.Group;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AmountThresholdRule implements ClassificationRule {

    private final ExpenseRepository expenseRepository;

    public AmountThresholdRule(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public String name() {
        return "amount-threshold";
    }

    @Override
    public double weight() {
        return 0.2;
    }

    @Override
    public Result evaluate(BankTransaction transaction, Group candidateGroup) {
        List<Long> amounts = expenseRepository.findAllByGroupIdOrderByCreatedAtDesc(candidateGroup.getId()).stream()
                .map(Expense::getAmount)
                .map(amount -> amount.cents())
                .sorted()
                .toList();
        if (amounts.isEmpty()) {
            return Result.noMatch();
        }

        long median = amounts.get(amounts.size() / 2);
        long tolerance = Math.max(500, Math.round(median * 0.5));
        long difference = Math.abs(transaction.getAmountCents() - median);
        if (difference > tolerance) {
            return Result.noMatch();
        }
        double score = 1 - ((double) difference / (tolerance + 1));
        return Result.score(score, "The amount is close to this group’s usual shared purchases");
    }
}
