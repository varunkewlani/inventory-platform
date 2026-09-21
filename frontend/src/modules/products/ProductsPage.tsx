import { useState, type FormEvent } from "react";
import { apiClient } from "../../services/apiClient";
import { usePaginatedResource } from "../../hooks/usePaginatedResource";
import type { ApiResponse } from "../../types/api";
import type { Product } from "./products.types";

export default function ProductsPage() {
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [editing, setEditing] = useState<Product | "new" | null>(null);
  // Bumped on every open/close so the form's `key` below always changes,
  // even when re-opening the same row or clicking Edit twice in a row —
  // without this, passing the same object/string back into setEditing can
  // be a no-op from React's point of view (Object.is same-value bailout),
  // which looked exactly like "clicking Edit does nothing."
  const [editSession, setEditSession] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function openEdit(target: Product | "new" | null) {
    setEditing(target);
    setEditSession((s) => s + 1);
  }

  const { page, loading, error, reload } = usePaginatedResource<Product>("/products", {
    page: 1,
    limit: 20,
    search: search || undefined,
  });

  function handleSearchSubmit(e: FormEvent) {
    e.preventDefault();
    setSearch(searchInput);
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    const body = {
      sku: form.get("sku"),
      name: form.get("name"),
      description: form.get("description") || undefined,
      price: Number(form.get("price")),
    };
    try {
      if (editing && editing !== "new") {
        await apiClient.patch(`/products/${editing.id}`, body);
      } else {
        await apiClient.post("/products", body);
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

  async function handleToggleStatus(product: Product) {
    const nextStatus = product.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    await apiClient.patch(`/products/${product.id}`, { status: nextStatus });
    reload();
  }

  const editingProduct = editing !== "new" ? editing : null;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Products</h1>
        <button type="button" onClick={() => openEdit(editing ? null : "new")}>
          {editing ? "Cancel" : "New Product"}
        </button>
      </div>

      {editing && (
        <form className="inline-form" onSubmit={handleSubmit} key={`${editingProduct?.id ?? "new"}-${editSession}`}>
          <input name="sku" placeholder="SKU" defaultValue={editingProduct?.sku} required />
          <input name="name" placeholder="Name" defaultValue={editingProduct?.name} required />
          <input name="description" placeholder="Description (optional)" defaultValue={editingProduct?.description ?? ""} />
          <input
            name="price"
            type="number"
            step="0.01"
            min="0"
            placeholder="Price"
            defaultValue={editingProduct?.price}
            required
          />
          <button type="submit" disabled={submitting}>
            {submitting ? "Saving…" : editingProduct ? "Save" : "Create"}
          </button>
          {formError && <p className="error-text">{formError}</p>}
        </form>
      )}

      <form className="search-bar" onSubmit={handleSearchSubmit}>
        <input
          type="search"
          placeholder="Search by name or SKU…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>

      {loading && <p>Loading…</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && page && (
        page.content.length === 0 ? (
          <p className="empty-state">No products found.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Name</th>
                <th>Price</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((product) => (
                <tr key={product.id}>
                  <td>{product.sku}</td>
                  <td>{product.name}</td>
                  <td>${product.price.toFixed(2)}</td>
                  <td>
                    <span className={`status-badge status-${product.status.toLowerCase()}`}>{product.status}</span>
                  </td>
                  <td>
                    <button type="button" className="link-button" onClick={() => openEdit(product)}>
                      Edit
                    </button>
                    {" · "}
                    <button type="button" className="link-button" onClick={() => handleToggleStatus(product)}>
                      {product.status === "ACTIVE" ? "Disable" : "Enable"}
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
