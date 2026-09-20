package com.uphead.inventory.users.dto;

import com.uphead.inventory.roles.Role;
import com.uphead.inventory.users.User;
import com.uphead.inventory.users.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        Long organizationId,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getOrganizationId(),
                user.getCreatedAt()
        );
    }
}
