import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext.jsx';
import { RequireAuth, RequireRole } from './auth/Guards.jsx';
import LoginPage from './pages/LoginPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import AbsencesPage from './pages/AbsencesPage.jsx';
import ValidationPage from './pages/ValidationPage.jsx';
import ProjetsPage from './pages/ProjetsPage.jsx';
import AdminPage from './pages/AdminPage.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<RequireAuth />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/absences" element={<AbsencesPage />} />
            <Route
              path="/validation"
              element={
                <RequireRole roles={['MANAGER', 'ADMIN']}>
                  <ValidationPage />
                </RequireRole>
              }
            />
            <Route path="/projets" element={<ProjetsPage />} />
            <Route
              path="/admin"
              element={
                <RequireRole roles={['ADMIN']}>
                  <AdminPage />
                </RequireRole>
              }
            />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
