package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.group.Group;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassificationEngine {

    private final List<ClassificationRule> rules;

    public ClassificationEngine(List<ClassificationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Result classify(BankTransaction transaction, Group candidateGroup) {
        double weightedScore = 0;
        double totalWeight = 0;
        List<String> reasons = new java.util.ArrayList<>();

        for (ClassificationRule rule : rules) {
            ClassificationRule.Result result = rule.evaluate(transaction, candidateGroup);
            if (result.suppress()) {
                return new Result(0, List.of(result.reason()), true);
            }
            weightedScore += result.score() * rule.weight();
            totalWeight += rule.weight();
            if (result.score() > 0 && !result.reason().isBlank()) {
                reasons.add(result.reason());
            }
        }

        double confidence = totalWeight == 0 ? 0 : Math.min(1, weightedScore / totalWeight);
        return new Result(confidence, List.copyOf(reasons), false);
    }

    public record Result(double confidence, List<String> reasons, boolean suppressed) {
    }
}
