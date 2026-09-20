package com.inventoryplatform.roles;

import com.inventoryplatform.common.exception.ForbiddenException;
import com.inventoryplatform.common.exception.UnauthorizedException;
import com.inventoryplatform.common.security.UserPrincipal;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!RolePermissions.has(principal.role(), requiresPermission.value())) {
            throw new ForbiddenException(
                    "Role " + principal.role() + " does not have permission " + requiresPermission.value());
        }
    }
}
