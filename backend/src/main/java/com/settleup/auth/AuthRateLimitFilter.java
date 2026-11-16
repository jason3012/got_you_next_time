package com.settleup.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleup.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 10;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !request.getRequestURI().matches("/auth/(login|register|refresh)");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= WINDOW_SECONDS
                        ? new Window(now, 1) : new Window(current.startedAt, current.count + 1));
        if (window.count > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
            objectMapper.writeValue(response.getOutputStream(), new ApiError(
                    Instant.now(), 429, "RATE_LIMITED", "Too many attempts. Wait a minute and try again.", Map.of()));
            return;
        }
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_SECONDS);
        chain.doFilter(request, response);
    }

    private record Window(long startedAt, int count) {}
}
