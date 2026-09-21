import { useCallback, useEffect, useState } from "react";
import { apiClient } from "../services/apiClient";
import type { ApiResponse, Page } from "../types/api";

/**
 * A reload driven by a dependency-array effect (params + an explicit
 * reload() call bump a counter) rather than imperatively calling a `load()`
 * function from inside event handlers — the latter pattern is what every
 * page previously used, and while it read correctly, it left room for
 * exactly the kind of "doesn't refresh until I reload the page" bug that
 *'s easy to introduce (stale closures, a missed call site, an early return
 * before the reload). Driving it from useEffect's dependency array makes a
 * missed refresh structurally harder, not just something to remember.
 */
export function usePaginatedResource<T>(url: string, params: Record<string, unknown> = {}) {
  const [page, setPage] = useState<Page<T> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  const paramsKey = JSON.stringify(params);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    apiClient
      .get<ApiResponse<Page<T>>>(url, { params: JSON.parse(paramsKey) })
      .then((res) => {
        if (!cancelled && res.data.data) setPage(res.data.data);
      })
      .catch(() => {
        if (!cancelled) setError("Failed to load data");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, paramsKey, reloadKey]);

  return { page, loading, error, reload };
}
