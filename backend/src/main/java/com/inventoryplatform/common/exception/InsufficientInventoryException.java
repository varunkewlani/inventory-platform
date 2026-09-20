package com.inventoryplatform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an atomic conditional UPDATE affects 0 rows because the
 * requested quantity isn't available. Lives in {@code common.exception}
 * (not the {@code inventory} package) since both {@code inventory} and
 * {@code orders} throw it.
 */
public class InsufficientInventoryException extends ApiException {

    public InsufficientInventoryException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY", message);
    }
}
