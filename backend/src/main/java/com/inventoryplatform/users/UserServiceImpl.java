package com.inventoryplatform.users;

import com.inventoryplatform.common.exception.ConflictException;
import com.inventoryplatform.common.exception.NotFoundException;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.users.dto.CreateUserRequest;
import com.inventoryplatform.users.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_TAKEN", "A user with this email already exists");
        }

        User user = User.builder()
                .organizationId(TenantContext.getOrganizationId())
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAllByOrganizationId(TenantContext.getOrganizationId(), pageable)
                .map(UserResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findTenantScoped(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserResponse.from(findTenantScoped(TenantContext.getUserId()));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = findTenantScoped(id);
        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }

    private User findTenantScoped(Long id) {
        return userRepository.findByIdAndOrganizationId(id, TenantContext.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
