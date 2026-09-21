import { useEffect, useState, type FormEvent } from "react";
import { apiClient } from "../../services/apiClient";
import type { ApiResponse, Page } from "../../types/api";
import type { Product } from "./products.types";

export default function ProductsPage() {
  const [page, setPage] = useState<Page<Product> | null>(null);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load(searchTerm: string) {
    setLoading(true);
    setError(null);
    apiClient
      .get<ApiResponse<Page<Product>>>("/products", { params: { page: 1, limit: 20, search: searchTerm || undefined } })
      .then((res) => {
        if (res.data.data) setPage(res.data.data);
      })
      .catch(() => setError("Failed to load products"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleSearchSubmit(e: FormEvent) {
    e.preventDefault();
    load(search);
  }

  async function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    try {
      await apiClient.post("/products", {
        sku: form.get("sku"),
        name: form.get("name"),
        description: form.get("description") || undefined,
        price: Number(form.get("price")),
      });
      setShowForm(false);
      e.currentTarget.reset();
      load(search);
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: ApiResponse<unknown> } })?.response?.data?.error?.message ??
        "Failed to create product";
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(product: Product) {
    const nextStatus = product.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    await apiClient.patch(`/products/${product.id}`, { status: nextStatus });
    load(search);
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Products</h1>
        <button type="button" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "New Product"}
        </button>
      </div>

      {showForm && (
        <form className="inline-form" onSubmit={handleCreate}>
          <input name="sku" placeholder="SKU" required />
          <input name="name" placeholder="Name" required />
          <input name="description" placeholder="Description (optional)" />
          <input name="price" type="number" step="0.01" min="0" placeholder="Price" required />
          <button type="submit" disabled={submitting}>
            {submitting ? "Creating…" : "Create"}
          </button>
          {formError && <p className="error-text">{formError}</p>}
        </form>
      )}

      <form className="search-bar" onSubmit={handleSearchSubmit}>
        <input
          type="search"
          placeholder="Search by name or SKU…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
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
