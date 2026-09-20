import { NavLink, Outlet } from "react-router-dom";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/products", label: "Products" },
  { to: "/warehouses", label: "Warehouses" },
  { to: "/inventory", label: "Inventory" },
  { to: "/orders", label: "Orders" },
];

export default function AppLayout() {
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
      </nav>
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
}
