package com.inventoryplatform.users.dto;

import com.inventoryplatform.users.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
