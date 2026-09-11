package com.settleup.user;

import com.settleup.auth.AuthenticatedUser;
import com.settleup.common.exception.NotFoundException;
import com.settleup.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userRepository.findById(AuthenticatedUser.id(jwt))
                .orElseThrow(() -> new NotFoundException("User was not found"));
        return UserResponse.from(user);
    }
}
