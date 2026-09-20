package com.inventoryplatform.users;

import com.inventoryplatform.users.dto.CreateUserRequest;
import com.inventoryplatform.users.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    Page<UserResponse> list(Pageable pageable);

    UserResponse getById(Long id);

    UserResponse getCurrentUser();

    UserResponse updateStatus(Long id, UserStatus status);
}
