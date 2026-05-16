package com.goodfunds.security;

import com.goodfunds.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public class AuthenticatedUser extends org.springframework.security.core.userdetails.User {

    private final UUID userId;
    private final String displayName;

    public AuthenticatedUser(User user) {
        super(
                user.getEmail(),
                user.getSenha(),
                user.isEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
        this.userId = user.getId();
        this.displayName = user.getNome();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
