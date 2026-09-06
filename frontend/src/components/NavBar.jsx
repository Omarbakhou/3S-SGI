import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

export default function NavBar() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <header className="navbar">
      <div className="navbar-brand">SGI</div>
      <nav className="navbar-links">
        <NavLink to="/dashboard" className={({ isActive }) => (isActive ? 'active' : '')}>
          Tableau de bord
        </NavLink>
        <NavLink to="/projets" className={({ isActive }) => (isActive ? 'active' : '')}>
          Imputations
        </NavLink>
        {hasRole('MANAGER', 'ADMIN') && (
          <NavLink to="/validation" className={({ isActive }) => (isActive ? 'active' : '')}>
            Validation
          </NavLink>
        )}
        {/* Un ADMIN est aussi un MANAGER (email listé dans ADMIN_EMAILS) : il faut l'exclure
            explicitement, hasRole('MANAGER') seul ne suffit pas. Un ADMIN ne dépose pas
            d'absence, il valide celles des managers. */}
        {!hasRole('ADMIN') && hasRole('EMPLOYE', 'MANAGER') && (
          <NavLink to="/absences" className={({ isActive }) => (isActive ? 'active' : '')}>
            Mes absences
          </NavLink>
        )}
        {hasRole('ADMIN') && (
          <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
            Administration
          </NavLink>
        )}
      </nav>
      <div className="navbar-user">
        <span className="navbar-email">{user?.email}</span>
        <span className="navbar-roles">{user?.roles?.join(', ')}</span>
        <button type="button" onClick={handleLogout} className="btn btn-secondary">
          Déconnexion
        </button>
      </div>
    </header>
  );
}
