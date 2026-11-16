package com.settleup.auth;

import com.settleup.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registrationAndLoginProtectUserDataEndToEnd() throws Exception {
        String token = register("alex@example.com", "Alex");

        mockMvc.perform(get("/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String loginToken = login("ALEX@example.com");
        mockMvc.perform(get("/groups").header("Authorization", bearer(loginToken)))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsAndMissingTokenAreRejected() throws Exception {
        register("alex@example.com", "Alex");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void refreshTokenRenewsTheSession() throws Exception {
        var registration = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"refresh@example.com\",\"displayName\":\"Refresh\",\"password\":\"password-123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();
        Cookie refreshCookie = registration.getResponse().getCookie("settleup-refresh");

        mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.user.email").value("refresh@example.com"));
    }
}
