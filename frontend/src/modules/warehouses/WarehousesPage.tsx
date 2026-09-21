import { useEffect, useState, type FormEvent } from "react";
import { apiClient } from "../../services/apiClient";
import type { ApiResponse, Page } from "../../types/api";
import type { Warehouse } from "./warehouses.types";

export default function WarehousesPage() {
  const [page, setPage] = useState<Page<Warehouse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<ApiResponse<Page<Warehouse>>>("/warehouses", { params: { page: 1, limit: 20 } })
      .then((res) => {
        if (res.data.data) setPage(res.data.data);
      })
      .catch(() => setError("Failed to load warehouses"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    try {
      await apiClient.post("/warehouses", {
        name: form.get("name"),
        address: form.get("address") || undefined,
      });
      setShowForm(false);
      e.currentTarget.reset();
      load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: ApiResponse<unknown> } })?.response?.data?.error?.message ??
        "Failed to create warehouse";
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(warehouse: Warehouse) {
    const nextStatus = warehouse.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    await apiClient.patch(`/warehouses/${warehouse.id}`, { status: nextStatus });
    load();
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Warehouses</h1>
        <button type="button" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "New Warehouse"}
        </button>
      </div>

      {showForm && (
        <form className="inline-form" onSubmit={handleCreate}>
          <input name="name" placeholder="Name" required />
          <input name="address" placeholder="Address (optional)" />
          <button type="submit" disabled={submitting}>
            {submitting ? "Creating…" : "Create"}
          </button>
          {formError && <p className="error-text">{formError}</p>}
        </form>
      )}

      {loading && <p>Loading…</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && page && (
        page.content.length === 0 ? (
          <p className="empty-state">No warehouses found.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Address</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((warehouse) => (
                <tr key={warehouse.id}>
                  <td>{warehouse.name}</td>
                  <td>{warehouse.address ?? "—"}</td>
                  <td>
                    <span className={`status-badge status-${warehouse.status.toLowerCase()}`}>{warehouse.status}</span>
                  </td>
                  <td>
                    <button type="button" className="link-button" onClick={() => handleToggleStatus(warehouse)}>
                      {warehouse.status === "ACTIVE" ? "Disable" : "Enable"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      )}
    </div>
  );
}
