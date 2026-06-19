package com.notify.identity.infrastructure.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(Long userId, String email, String name, Collection<? extends GrantedAuthority> authorities,
        boolean enabled) implements UserDetails {

    public UserPrincipal(Long userId, String email, String name, Collection<? extends GrantedAuthority> authorities,
            boolean enabled) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.authorities = authorities != null ? List.copyOf(authorities) : List.of();
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
