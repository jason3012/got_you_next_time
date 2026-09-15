package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.bank.BankTransactionRepository;
import com.settleup.group.Group;
import org.springframework.stereotype.Component;

@Component
public class MerchantHistoryRule implements ClassificationRule {

    private final BankTransactionRepository transactionRepository;
    private final ExpenseSuggestionRepository suggestionRepository;

    public MerchantHistoryRule(
            BankTransactionRepository transactionRepository,
            ExpenseSuggestionRepository suggestionRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.suggestionRepository = suggestionRepository;
    }

    @Override
    public String name() {
        return "merchant-history";
    }

    @Override
    public double weight() {
        return 0.55;
    }

    @Override
    public Result evaluate(BankTransaction transaction, Group candidateGroup) {
        String merchant = merchant(transaction);
        if (suggestionRepository.existsRejectedMerchantForGroup(candidateGroup.getId(), merchant)) {
            return Result.suppress("You previously said this merchant was not for this group");
        }
        if (transactionRepository.existsConfirmedMerchantForGroup(candidateGroup.getId(), merchant)) {
            return Result.score(1, "You previously shared a purchase from " + merchant + " with this group");
        }
        return Result.noMatch();
    }

    private String merchant(BankTransaction transaction) {
        return transaction.getMerchantName() == null ? transaction.getName() : transaction.getMerchantName();
    }
}
