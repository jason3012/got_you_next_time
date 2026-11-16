package com.settleup.auth;

import com.settleup.auth.dto.AuthResponse;
import com.settleup.auth.dto.LoginRequest;
import com.settleup.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean secureCookie;
    private final Duration refreshTtl;

    public AuthController(
            AuthService authService,
            @Value("${security.refresh-cookie-secure:false}") boolean secureCookie,
            @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl
    ) {
        this.authService = authService;
        this.secureCookie = secureCookie;
        this.refreshTtl = refreshTtl;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(withoutRefreshToken(auth));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setRefreshCookie(response, auth.refreshToken());
        return withoutRefreshToken(auth);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue("settleup-refresh") String refreshToken, HttpServletResponse response) {
        AuthResponse auth = authService.refresh(refreshToken);
        setRefreshCookie(response, auth.refreshToken());
        return withoutRefreshToken(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "settleup-refresh", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("").maxAge(0).build().toString());
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).maxAge(refreshTtl).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookie(String value) {
        return ResponseCookie.from("settleup-refresh", value).httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path("/");
    }

    private AuthResponse withoutRefreshToken(AuthResponse auth) {
        return new AuthResponse(auth.accessToken(), null, auth.tokenType(), auth.expiresInSeconds(), auth.user());
    }
}
