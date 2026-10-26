package com.spotifyclone.security;

public record AuthUser(Long id, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
