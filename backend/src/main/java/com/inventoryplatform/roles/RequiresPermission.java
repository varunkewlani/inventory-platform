package com.inventoryplatform.roles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the permission a controller/service method requires. Enforced by
 * {@link PermissionAspect} against the role carried on the authenticated
 * principal — this is backend enforcement, independent of anything the
 * frontend hides.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresPermission {
    Permission value();
}
