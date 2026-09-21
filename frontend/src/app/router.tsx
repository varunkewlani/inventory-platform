import { createBrowserRouter } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import ProtectedRoute from "../components/ProtectedRoute";
import LoginPage from "../modules/auth/LoginPage";
import DashboardPage from "../modules/dashboard/DashboardPage";
import ProductsPage from "../modules/products/ProductsPage";
import WarehousesPage from "../modules/warehouses/WarehousesPage";
import InventoryPage from "../modules/inventory/InventoryPage";
import OrdersPage from "../modules/orders/OrdersPage";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
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
    ],
  },
]);
