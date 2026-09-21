import { useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";
import type { ApiResponse } from "../../types/api";
import type { DashboardData } from "./dashboard.types";

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    apiClient
      .get<ApiResponse<DashboardData>>("/dashboard")
      .then((res) => {
        if (!cancelled && res.data.data) setData(res.data.data);
      })
      .catch(() => {
        if (!cancelled) setError("Failed to load dashboard");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) return <div className="page"><p>Loading dashboard…</p></div>;
  if (error) return <div className="page"><p className="error-text">{error}</p></div>;
  if (!data) return null;

  const widgets = [
    { label: "Total Products", value: data.totalProducts },
    { label: "Total Warehouses", value: data.totalWarehouses },
    { label: "Available Inventory", value: data.availableInventory },
    { label: "Pending Orders", value: data.pendingOrders },
    { label: "Completed Orders", value: data.completedOrders },
    { label: "Low Stock Products", value: data.lowStockProducts },
  ];

  return (
    <div className="page">
      <h1>Dashboard</h1>

      <div className="widget-grid">
        {widgets.map((w) => (
          <div className="widget-card" key={w.label}>
            <div className="widget-value">{w.value}</div>
            <div className="widget-label">{w.label}</div>
          </div>
        ))}
      </div>

      <h2>Recent Orders</h2>
      {data.recentOrders.length === 0 ? (
        <p className="empty-state">No orders yet.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Status</th>
              <th>Total</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {data.recentOrders.map((order) => (
              <tr key={order.id}>
                <td>#{order.id}</td>
                <td>
                  <span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status}</span>
                </td>
                <td>${order.totalAmount.toFixed(2)}</td>
                <td>{new Date(order.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
