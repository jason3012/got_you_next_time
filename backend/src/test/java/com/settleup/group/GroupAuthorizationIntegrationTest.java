package com.settleup.group;

import com.settleup.expense.Expense;
import com.settleup.support.AbstractIntegrationTest;
import com.settleup.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void outsiderCannotReadGroupAndMemberCannotAdministerIt() throws Exception {
        User admin = data.user("Admin");
        User member = data.user("Member");
        User outsider = data.user("Outsider");
        Group group = data.group("Trip", admin, member);

        mockMvc.perform(get("/groups/{id}", group.getId())
                        .header("Authorization", bearer(login(outsider.getEmail()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/groups/{id}/members", group.getId())
                        .header("Authorization", bearer(login(member.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"outsider@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyCreatorOrAdminCanDeleteExpense() throws Exception {
        User admin = data.user("Admin");
        User creator = data.user("Creator");
        User member = data.user("Member");
        Group group = data.group("Trip", admin, creator, member);
        Expense expense = data.equalExpense(group, creator, creator, 1200, admin, creator, member);

        mockMvc.perform(delete("/groups/{groupId}/expenses/{expenseId}", group.getId(), expense.getId())
                        .header("Authorization", bearer(login(member.getEmail()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/groups/{groupId}/expenses/{expenseId}", group.getId(), expense.getId())
                        .header("Authorization", bearer(login(admin.getEmail()))))
                .andExpect(status().isNoContent());
    }
}
