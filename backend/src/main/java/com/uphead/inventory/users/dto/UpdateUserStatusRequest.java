package com.uphead.inventory.users.dto;

import com.uphead.inventory.users.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
