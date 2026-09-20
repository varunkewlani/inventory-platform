package com.uphead.inventory.users;

import com.uphead.inventory.users.dto.CreateUserRequest;
import com.uphead.inventory.users.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    Page<UserResponse> list(Pageable pageable);

    UserResponse getById(Long id);

    UserResponse getCurrentUser();

    UserResponse updateStatus(Long id, UserStatus status);
}
