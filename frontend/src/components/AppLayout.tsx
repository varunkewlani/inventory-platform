import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../store/AuthContext";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/products", label: "Products" },
  { to: "/warehouses", label: "Warehouses" },
  { to: "/inventory", label: "Inventory" },
  { to: "/orders", label: "Orders" },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-layout">
      <nav className="app-nav">
        <strong>Inventory Platform</strong>
        <ul>
          {navItems.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to} end={item.to === "/"}>
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
        <div className="app-nav-footer">
          {user && (
            <div className="app-nav-user">
              <div className="app-nav-user-name">{user.name}</div>
              <div className="app-nav-user-role">{user.role}</div>
            </div>
          )}
          <button type="button" className="app-nav-logout" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </nav>
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
}
