package com.settleup.expense;

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

class ExpenseCrudIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsAllSplitStrategiesAgainstPostgres() throws Exception {
        User alex = data.user("Alex");
        User blair = data.user("Blair");
        Group group = data.group("House", alex, blair);
        String token = login(alex.getEmail());

        create(group, token, "Dinner", 1001, "EQUAL", """
                [{"userId":"%s"},{"userId":"%s"}]
                """.formatted(alex.getId(), blair.getId()));
        create(group, token, "Tickets", 1500, "EXACT", """
                [{"userId":"%s","amountCents":1000},{"userId":"%s","amountCents":500}]
                """.formatted(alex.getId(), blair.getId()));
        create(group, token, "Rental", 2000, "PERCENTAGE", """
                [{"userId":"%s","percentage":25},{"userId":"%s","percentage":75}]
                """.formatted(alex.getId(), blair.getId()));

        mockMvc.perform(get("/groups/{id}/expenses", group.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.description == 'Dinner')].splits[*].shareCents",
                        org.hamcrest.Matchers.containsInAnyOrder(500, 501)));
    }

    @Test
    void rejectsAnExactSplitThatDoesNotBalance() throws Exception {
        User alex = data.user("Alex");
        User blair = data.user("Blair");
        Group group = data.group("House", alex, blair);

        mockMvc.perform(post("/groups/{id}/expenses", group.getId())
                        .header("Authorization", bearer(login(alex.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Dinner", 1000, alex, "EXACT", """
                                [{"userId":"%s","amountCents":400},{"userId":"%s","amountCents":500}]
                                """.formatted(alex.getId(), blair.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private void create(Group group, String token, String description, long cents, String strategy, String splits)
            throws Exception {
        User payer = group.getCreatedBy();
        mockMvc.perform(post("/groups/{id}/expenses", group.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(description, cents, payer, strategy, splits)))
                .andExpect(status().isCreated());
    }

    private String expenseJson(String description, long cents, User payer, String strategy, String splits)
            throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("description", description)
                .put("amountCents", cents)
                .put("payerId", payer.getId().toString())
                .put("splitStrategy", strategy)
                .set("splits", objectMapper.readTree(splits)));
    }
}
