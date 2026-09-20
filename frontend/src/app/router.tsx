import { createBrowserRouter } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import LoginPage from "../modules/auth/LoginPage";
import DashboardPage from "../modules/dashboard/DashboardPage";
import ProductsPage from "../modules/products/ProductsPage";
import WarehousesPage from "../modules/warehouses/WarehousesPage";
import InventoryPage from "../modules/inventory/InventoryPage";
import OrdersPage from "../modules/orders/OrdersPage";

// Route guards (redirect to /login when unauthenticated) land in Phase 6
// alongside the real AuthContext.
export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "products", element: <ProductsPage /> },
      { path: "warehouses", element: <WarehousesPage /> },
      { path: "inventory", element: <InventoryPage /> },
      { path: "orders", element: <OrdersPage /> },
    ],
  },
]);
