package com.spotifyclone.web;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.security.AuthUser;
import com.spotifyclone.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser u) {
        return auth.me(u.id());
    }

    @PutMapping("/me")
    public UserDto update(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody UpdateProfileRequest r) {
        return auth.updateProfile(u.id(), r);
    }
}
