package com.inventoryplatform.auth;

import com.inventoryplatform.audit.AuditService;
import com.inventoryplatform.auth.dto.AuthResult;
import com.inventoryplatform.auth.dto.AuthTokens;
import com.inventoryplatform.auth.dto.LoginRequest;
import com.inventoryplatform.auth.dto.RegisterRequest;
import com.inventoryplatform.common.exception.ConflictException;
import com.inventoryplatform.common.exception.UnauthorizedException;
import com.inventoryplatform.organizations.Organization;
import com.inventoryplatform.organizations.OrganizationRepository;
import com.inventoryplatform.roles.Role;
import com.inventoryplatform.users.User;
import com.inventoryplatform.users.UserRepository;
import com.inventoryplatform.users.UserStatus;
import com.inventoryplatform.users.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request, String deviceInfo) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_TAKEN", "A user with this email already exists");
        }

        Organization organization = organizationRepository.save(
                Organization.builder().name(request.organizationName()).build());

        User admin = User.builder()
                .organizationId(organization.getId())
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        admin = userRepository.save(admin);

        auditService.log(organization.getId(), admin.getId(), "USER_CREATED", "User", admin.getId().toString(),
                null, UserResponse.from(admin));

        return new AuthResult(UserResponse.from(admin), issueTokens(admin, deviceInfo));
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request, String deviceInfo) {
        // Not tenant-scoped: at login time we don't yet know which
        // organization the caller belongs to — that's what this lookup
        // determines. Email is global specifically to make this resolvable.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("This account has been disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        auditService.log(user.getOrganizationId(), user.getId(), "LOGIN", "User", user.getId().toString(), null, null);

        return new AuthResult(UserResponse.from(user), issueTokens(user, deviceInfo));
    }

    @Override
    // Deliberately not @Transactional: RefreshTokenService.rotate() commits
    // its reuse-detection revocation independently and must not be pulled
    // into a transaction that a later exception here would roll back.
    public AuthTokens refresh(String rawRefreshToken, String deviceInfo) {
        Long userId = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("This account has been disabled");
        }

        return issueTokens(user, deviceInfo);
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthTokens issueTokens(User user, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getOrganizationId(), user.getRole(), user.getEmail());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user.getId(), deviceInfo);
        return new AuthTokens(accessToken, refreshToken.rawToken(), refreshToken.expiresAt());
    }
}
