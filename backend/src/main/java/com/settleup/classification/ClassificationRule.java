package com.settleup.classification;

import com.settleup.bank.BankTransaction;
import com.settleup.group.Group;

public interface ClassificationRule {

    String name();

    double weight();

    Result evaluate(BankTransaction transaction, Group candidateGroup);

    record Result(double score, String reason, boolean suppress) {
        public Result {
            if (score < 0 || score > 1) {
                throw new IllegalArgumentException("Classification scores must be between 0 and 1");
            }
        }

        public static Result score(double score, String reason) {
            return new Result(score, reason, false);
        }

        public static Result noMatch() {
            return new Result(0, "", false);
        }

        public static Result suppress(String reason) {
            return new Result(0, reason, true);
        }
    }
}
