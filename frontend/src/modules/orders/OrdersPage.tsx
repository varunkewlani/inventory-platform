import { useEffect, useState, type FormEvent } from "react";
import { apiClient } from "../../services/apiClient";
import type { ApiResponse, Page } from "../../types/api";
import type { Order, OrderStatus } from "./orders.types";
import { NEXT_STATUS } from "./orders.types";
import type { Warehouse } from "../warehouses/warehouses.types";
import type { Product } from "../products/products.types";

interface Customer {
  id: number;
  name: string;
}

export default function OrdersPage() {
  const [page, setPage] = useState<Page<Order> | null>(null);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<ApiResponse<Page<Order>>>("/orders", { params: { page: 1, limit: 20, sort: "createdAt:desc" } })
      .then((res) => {
        if (res.data.data) setPage(res.data.data);
      })
      .catch(() => setError("Failed to load orders"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
    apiClient.get<ApiResponse<Page<Warehouse>>>("/warehouses", { params: { page: 1, limit: 100 } })
      .then((res) => setWarehouses(res.data.data?.content ?? []));
    apiClient.get<ApiResponse<Page<Product>>>("/products", { params: { page: 1, limit: 100 } })
      .then((res) => setProducts(res.data.data?.content ?? []));
    apiClient.get<ApiResponse<Page<Customer>>>("/customers", { params: { page: 1, limit: 100 } })
      .then((res) => setCustomers(res.data.data?.content ?? []))
      .catch(() => setCustomers([]));
  }, []);

  async function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    try {
      let customerId = form.get("customerId");
      if (customerId === "new") {
        const name = form.get("newCustomerName");
        const createRes = await apiClient.post<ApiResponse<Customer>>("/customers", { name });
        customerId = String(createRes.data.data?.id);
      }

      await apiClient.post("/orders", {
        warehouseId: Number(form.get("warehouseId")),
        customerId: Number(customerId),
        items: [{ productId: Number(form.get("productId")), quantity: Number(form.get("quantity")) }],
      });
      setShowForm(false);
      e.currentTarget.reset();
      load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: ApiResponse<unknown> } })?.response?.data?.error?.message ??
        "Failed to create order";
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusChange(order: Order, status: OrderStatus) {
    try {
      await apiClient.patch(`/orders/${order.id}/status`, { status });
      load();
    } catch {
      setError(`Failed to update order #${order.id}`);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Orders</h1>
        <button type="button" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "New Order"}
        </button>
      </div>

      {showForm && (
        <form className="inline-form" onSubmit={handleCreate}>
          <select name="warehouseId" required defaultValue="">
            <option value="" disabled>Warehouse…</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
          <select name="customerId" required defaultValue="">
            <option value="" disabled>Customer…</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
            <option value="new">+ New customer…</option>
          </select>
          <input name="newCustomerName" placeholder="New customer name" />
          <select name="productId" required defaultValue="">
            <option value="" disabled>Product…</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>{p.sku} — {p.name}</option>
            ))}
          </select>
          <input name="quantity" type="number" min="1" placeholder="Quantity" required />
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
          <p className="empty-state">No orders found.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Status</th>
                <th>Total</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((order) => (
                <tr key={order.id}>
                  <td>#{order.id}</td>
                  <td>
                    <span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status}</span>
                  </td>
                  <td>${order.totalAmount.toFixed(2)}</td>
                  <td>{new Date(order.createdAt).toLocaleString()}</td>
                  <td>
                    {NEXT_STATUS[order.status].map((next) => (
                      <button
                        key={next}
                        type="button"
                        className="link-button"
                        style={{ marginRight: 8 }}
                        onClick={() => handleStatusChange(order, next)}
                      >
                        → {next}
                      </button>
                    ))}
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
