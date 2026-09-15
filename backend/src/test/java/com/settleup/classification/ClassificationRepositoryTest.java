package com.settleup.classification;

import com.settleup.bank.BankTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ClassificationRepositoryTest {

    @Autowired
    private ExpenseSuggestionRepository suggestionRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Test
    void classificationMappingsAndQueriesLoad() {
        assertThat(suggestionRepository).isNotNull();
        assertThat(transactionRepository).isNotNull();
    }
}
