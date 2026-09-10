package com.settleup.common;

import java.util.Objects;

public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Cents value is outside the supported range");
        }
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money minus(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(Math.subtractExact(cents, other.cents));
    }

    public Money negate() {
        return new Money(Math.negateExact(cents));
    }

    public boolean isZero() {
        return cents == 0;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return Long.compare(cents, other.cents);
    }

    public String formatUsd() {
        long absoluteCents = Math.abs(cents);
        String sign = cents < 0 ? "-" : "";
        return sign + "$" + (absoluteCents / 100) + ".%02d".formatted(absoluteCents % 100);
    }
}
