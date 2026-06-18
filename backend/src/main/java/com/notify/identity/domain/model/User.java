package com.notify.identity.domain.model;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {

    private UserId id;

    private Email email;

    private String name;

    private PasswordHash password;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Long tokenVersion = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private Set<RoleName> roles = EnumSet.noneOf(RoleName.class);

    public void setPassword(PasswordHash password) {
        this.password = password;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean hasRole(RoleName roleName) {
        return roles.contains(roleName);
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    public void updateProfile(String newName, Email newEmail) {
        this.name = newName;
        this.email = newEmail;
    }
}
