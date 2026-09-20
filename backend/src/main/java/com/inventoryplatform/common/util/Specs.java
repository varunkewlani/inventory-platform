package com.inventoryplatform.common.util;

import org.springframework.data.jpa.domain.Specification;

/**
 * Null-safe {@link Specification} combination. This Spring Data version's
 * own {@code Specification.and(...)} throws on a null argument rather than
 * treating it as "no predicate" — this is for the common case of
 * conditionally-present filters (search/status/etc. query params) where
 * absent means "don't filter on this", not an error.
 */
public final class Specs {

    private Specs() {
    }

    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specifications) {
        Specification<T> result = null;
        for (Specification<T> spec : specifications) {
            if (spec == null) {
                continue;
            }
            result = (result == null) ? spec : result.and(spec);
        }
        return result != null ? result : (root, query, cb) -> cb.conjunction();
    }
}
