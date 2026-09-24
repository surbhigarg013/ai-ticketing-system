import { Outlet, NavLink } from 'react-router-dom';

export function AppLayout() {
  return (
    <div className="app-shell">
      <nav className="app-nav">
        <NavLink to="/tickets">Tickets</NavLink>
        <NavLink to="/assistant">Assistant</NavLink>
      </nav>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
