package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.bank.BankTransactionRepository;
import com.settleup.group.Group;
import org.springframework.stereotype.Component;

@Component
public class CategoryRule implements ClassificationRule {

    private final BankTransactionRepository transactionRepository;

    public CategoryRule(BankTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String name() {
        return "category";
    }

    @Override
    public double weight() {
        return 0.25;
    }

    @Override
    public Result evaluate(BankTransaction transaction, Group candidateGroup) {
        if (transaction.getCategory() == null || transaction.getCategory().isBlank()) {
            return Result.noMatch();
        }
        if (!transactionRepository.existsConfirmedCategoryForGroup(candidateGroup.getId(), transaction.getCategory())) {
            return Result.noMatch();
        }
        String readableCategory = transaction.getCategory().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Result.score(1, "This group often shares " + readableCategory + " purchases");
    }
}
