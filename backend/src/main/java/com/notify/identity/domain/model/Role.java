package com.notify.identity.domain.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Role {

    private Long id;

    private RoleName name;

    @Builder.Default
    private String description = "";

    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
