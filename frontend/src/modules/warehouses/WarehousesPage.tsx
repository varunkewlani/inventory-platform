import { useState, type FormEvent } from "react";
import { apiClient } from "../../services/apiClient";
import { usePaginatedResource } from "../../hooks/usePaginatedResource";
import type { ApiResponse } from "../../types/api";
import type { Warehouse } from "./warehouses.types";

export default function WarehousesPage() {
  const [editing, setEditing] = useState<Warehouse | "new" | null>(null);
  // Bumped on every open so the form's `key` below always changes, even when
  // re-opening the same row or clicking Edit twice in a row — without this,
  // passing the same object/string back into setEditing can be a no-op from
  // React's point of view (Object.is same-value bailout).
  const [editSession, setEditSession] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function openEdit(target: Warehouse | "new" | null) {
    setEditing(target);
    setEditSession((s) => s + 1);
  }

  const { page, loading, error, reload } = usePaginatedResource<Warehouse>("/warehouses", { page: 1, limit: 20 });

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    const body = {
      name: form.get("name"),
      address: form.get("address") || undefined,
    };
    try {
      if (editing && editing !== "new") {
        await apiClient.patch(`/warehouses/${editing.id}`, body);
      } else {
        await apiClient.post("/warehouses", body);
      }
      setEditing(null);
      reload();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: ApiResponse<unknown> } })?.response?.data?.error?.message ??
        "Save failed";
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(warehouse: Warehouse) {
    const nextStatus = warehouse.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    await apiClient.patch(`/warehouses/${warehouse.id}`, { status: nextStatus });
    reload();
  }

  const editingWarehouse = editing !== "new" ? editing : null;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Warehouses</h1>
        <button type="button" onClick={() => openEdit(editing ? null : "new")}>
          {editing ? "Cancel" : "New Warehouse"}
        </button>
      </div>

      {editing && (
        <form className="inline-form" onSubmit={handleSubmit} key={`${editingWarehouse?.id ?? "new"}-${editSession}`}>
          <input name="name" placeholder="Name" defaultValue={editingWarehouse?.name} required />
          <input name="address" placeholder="Address (optional)" defaultValue={editingWarehouse?.address ?? ""} />
          <button type="submit" disabled={submitting}>
            {submitting ? "Saving…" : editingWarehouse ? "Save" : "Create"}
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
                    <button type="button" className="link-button" onClick={() => openEdit(warehouse)}>
                      Edit
                    </button>
                    {" · "}
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
