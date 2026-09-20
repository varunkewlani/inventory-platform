package com.uphead.inventory.users;

import com.uphead.inventory.common.response.ApiResponse;
import com.uphead.inventory.roles.Permission;
import com.uphead.inventory.roles.RequiresPermission;
import com.uphead.inventory.users.dto.CreateUserRequest;
import com.uphead.inventory.users.dto.UpdateUserStatusRequest;
import com.uphead.inventory.users.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @RequiresPermission(Permission.USER_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @GetMapping
    @RequiresPermission(Permission.USER_READ)
    public ApiResponse<Page<UserResponse>> list(Pageable pageable) {
        return ApiResponse.success(userService.list(pageable));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.USER_READ)
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @RequiresPermission(Permission.USER_MANAGE)
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success(userService.updateStatus(id, request.status()));
    }
}
