import { useEffect, useState, type FormEvent } from "react";
import { apiClient } from "../../services/apiClient";
import type { ApiResponse, Page } from "../../types/api";
import type { InventoryRow } from "./inventory.types";
import type { Warehouse } from "../warehouses/warehouses.types";
import type { Product } from "../products/products.types";

export default function InventoryPage() {
  const [page, setPage] = useState<Page<InventoryRow> | null>(null);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<ApiResponse<Page<InventoryRow>>>("/inventory", { params: { page: 1, limit: 20 } })
      .then((res) => {
        if (res.data.data) setPage(res.data.data);
      })
      .catch(() => setError("Failed to load inventory"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
    apiClient
      .get<ApiResponse<Page<Warehouse>>>("/warehouses", { params: { page: 1, limit: 100 } })
      .then((res) => setWarehouses(res.data.data?.content ?? []));
    apiClient
      .get<ApiResponse<Page<Product>>>("/products", { params: { page: 1, limit: 100 } })
      .then((res) => setProducts(res.data.data?.content ?? []));
  }, []);

  async function handleAdjust(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    try {
      await apiClient.post("/inventory/adjust", {
        warehouseId: Number(form.get("warehouseId")),
        productId: Number(form.get("productId")),
        type: form.get("type"),
        quantity: Number(form.get("quantity")),
      });
      setShowForm(false);
      e.currentTarget.reset();
      load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: ApiResponse<unknown> } })?.response?.data?.error?.message ??
        "Adjustment failed";
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Inventory</h1>
        <button type="button" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "Adjust Stock"}
        </button>
      </div>

      {showForm && (
        <form className="inline-form" onSubmit={handleAdjust}>
          <select name="warehouseId" required defaultValue="">
            <option value="" disabled>Warehouse…</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
          <select name="productId" required defaultValue="">
            <option value="" disabled>Product…</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>{p.sku} — {p.name}</option>
            ))}
          </select>
          <select name="type" required defaultValue="ADD">
            <option value="ADD">Add</option>
            <option value="REMOVE">Remove</option>
          </select>
          <input name="quantity" type="number" min="1" placeholder="Quantity" required />
          <button type="submit" disabled={submitting}>
            {submitting ? "Saving…" : "Apply"}
          </button>
          {formError && <p className="error-text">{formError}</p>}
        </form>
      )}

      {loading && <p>Loading…</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && page && (
        page.content.length === 0 ? (
          <p className="empty-state">No inventory records found.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Warehouse</th>
                <th>Available</th>
                <th>Reserved</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((row) => (
                <tr key={row.id}>
                  <td>{row.productSku} — {row.productName}</td>
                  <td>{row.warehouseName}</td>
                  <td>{row.availableQuantity}</td>
                  <td>{row.reservedQuantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      )}
    </div>
  );
}
