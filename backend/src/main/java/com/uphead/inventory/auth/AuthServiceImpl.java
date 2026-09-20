package com.uphead.inventory.auth;

import com.uphead.inventory.auth.dto.AuthResult;
import com.uphead.inventory.auth.dto.AuthTokens;
import com.uphead.inventory.auth.dto.LoginRequest;
import com.uphead.inventory.auth.dto.RegisterRequest;
import com.uphead.inventory.common.exception.ConflictException;
import com.uphead.inventory.common.exception.UnauthorizedException;
import com.uphead.inventory.organizations.Organization;
import com.uphead.inventory.organizations.OrganizationRepository;
import com.uphead.inventory.roles.Role;
import com.uphead.inventory.users.User;
import com.uphead.inventory.users.UserRepository;
import com.uphead.inventory.users.UserStatus;
import com.uphead.inventory.users.dto.UserResponse;
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
