import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import NavBar from '../components/NavBar.jsx';

/** Requires a valid session; otherwise sends the visitor to /login. Renders the shared nav bar. */
export function RequireAuth() {
  const { token, ready } = useAuth();

  if (!ready) {
    return <div className="page-loading">Chargement…</div>;
  }
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return (
    <>
      <NavBar />
      <main className="page-content">
        <Outlet />
      </main>
    </>
  );
}

/** Requires the current user to hold at least one of the given roles; otherwise back to /dashboard. */
export function RequireRole({ roles, children }) {
  const { hasRole } = useAuth();
  if (!hasRole(...roles)) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}
