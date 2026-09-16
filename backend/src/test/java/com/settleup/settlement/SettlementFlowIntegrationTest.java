package com.settleup.settlement;

import com.settleup.group.Group;
import com.settleup.support.AbstractIntegrationTest;
import com.settleup.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void realisticExpenseProducesMinimalPlanAndPartialPaymentUpdatesBalances() throws Exception {
        User alex = data.user("Alex");
        User blair = data.user("Blair");
        User casey = data.user("Casey");
        Group group = data.group("Trip", alex, blair, casey);
        data.equalExpense(group, alex, alex, 900, alex, blair, casey);
        String token = login(alex.getEmail());

        mockMvc.perform(get("/groups/{id}/settle-up", group.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(post("/groups/{id}/settlements", group.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromUserId":"%s","toUserId":"%s","amountCents":150}
                                """.formatted(blair.getId(), alex.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/groups/{id}/balances", group.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '%s')].balanceCents".formatted(blair.getId())).value(-150))
                .andExpect(jsonPath("$[?(@.userId == '%s')].balanceCents".formatted(alex.getId())).value(450));
    }
}
