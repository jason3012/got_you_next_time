package com.settleup.auth;

import com.settleup.auth.dto.AuthResponse;
import com.settleup.auth.dto.LoginRequest;
import com.settleup.common.exception.UnauthorizedException;
import com.settleup.user.User;
import com.settleup.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    @Test
    void loginNormalizesEmailBeforeLookup() {
        User user = new User("friend@example.com", "Friend", "correct horse");
        AtomicReference<String> lookedUpEmail = new AtomicReference<>();
        AuthService service = service(repository(Optional.of(user), lookedUpEmail));

        AuthResponse response = service.login(
                new LoginRequest("  FRIEND@EXAMPLE.COM ", "correct horse"));

        assertThat(lookedUpEmail).hasValue("friend@example.com");
        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void loginRejectsUnknownEmail() {
        AuthService service = service(repository(Optional.empty(), new AtomicReference<>()));

        assertThatThrownBy(() -> service.login(
                new LoginRequest("missing@example.com", "incorrect")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email or password is incorrect");
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = new User("friend@example.com", "Friend", "correct horse");
        AuthService service = service(repository(Optional.of(user), new AtomicReference<>()));

        assertThatThrownBy(() -> service.login(
                new LoginRequest("friend@example.com", "incorrect")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email or password is incorrect");
    }

    private AuthService service(UserRepository repository) {
        return new AuthService(repository, new PlainTextPasswordEncoder(), new StubJwtService());
    }

    private UserRepository repository(Optional<User> result, AtomicReference<String> lookedUpEmail) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("findByEmailIgnoreCase")) {
                        lookedUpEmail.set((String) arguments[0]);
                        return result;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class PlainTextPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    }

    private static final class StubJwtService extends JwtService {
        private StubJwtService() {
            super(parameters -> null, Clock.systemUTC(), "test", Duration.ofHours(1));
        }

        @Override
        public IssuedToken issue(User user) {
            return new IssuedToken("token", 3600);
        }
    }
}
