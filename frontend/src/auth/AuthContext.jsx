import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, getToken, getStoredUser, setSession, clearSession } from '../api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(getToken());
  const [user, setUser] = useState(getStoredUser());
  const [ready, setReady] = useState(false);

  useEffect(() => {
    // Validate a session restored from sessionStorage against the backend
    // (also naturally handles an expired token via api.js's 401 handling).
    if (token && !user) {
      api
        .get('/api/auth/me')
        .then((me) => {
          setUser(me);
          setSession(token, me);
        })
        .catch(() => {
          setToken(null);
          setUser(null);
        })
        .finally(() => setReady(true));
    } else {
      setReady(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(async (email, motDePasse) => {
    const response = await api.post('/api/auth/login', { email, motDePasse });
    const loggedInUser = {
      id: response.id,
      email: response.email,
      nom: response.nom,
      prenom: response.prenom,
      roles: response.roles,
    };
    setSession(response.token, loggedInUser);
    setToken(response.token);
    setUser(loggedInUser);
    return loggedInUser;
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setToken(null);
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (...roles) => !!user && roles.some((role) => user.roles.includes(role)),
    [user]
  );

  return (
    <AuthContext.Provider value={{ token, user, ready, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

/** "Prénom Nom", replié sur l'email si nom/prénom sont absents. */
export function displayName(user) {
  if (!user) return '';
  const full = [user.prenom, user.nom].filter(Boolean).join(' ').trim();
  return full || user.email;
}