package com.spotifyclone.service;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.model.User;
import com.spotifyclone.repo.UserRepository;
import com.spotifyclone.security.JwtService;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (users.existsByEmailIgnoreCase(r.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (users.existsByUsernameIgnoreCase(r.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        User u = new User();
        u.username = r.username();
        u.email = r.email().toLowerCase();
        u.passwordHash = encoder.encode(r.password());
        users.save(u);
        return new AuthResponse(jwt.generate(u), UserDto.of(u));
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email())
                .filter(x -> encoder.matches(r.password(), x.passwordHash))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u), UserDto.of(u));
    }

    public UserDto me(Long id) {
        return UserDto.of(find(id));
    }

    @Transactional
    public UserDto updateProfile(Long id, UpdateProfileRequest r) {
        User u = find(id);
        users.findByEmailIgnoreCase(r.email()).filter(o -> !o.id.equals(id)).ifPresent(o -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        });
        if (!u.username.equalsIgnoreCase(r.username()) && users.existsByUsernameIgnoreCase(r.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        u.username = r.username();
        u.email = r.email().toLowerCase();
        if (r.newPassword() != null && !r.newPassword().isBlank()) {
            u.passwordHash = encoder.encode(r.newPassword());
        }
        return UserDto.of(u);
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
