package com.settleup.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money money) {
        return money == null ? null : money.cents();
    }

    @Override
    public Money convertToEntityAttribute(Long cents) {
        return cents == null ? null : new Money(cents);
    }
}
