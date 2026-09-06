const TOKEN_KEY = 'sgi.token';
const USER_KEY = 'sgi.user';

// In-memory copy so we don't hit sessionStorage on every request; sessionStorage
// is kept in sync so a page reload doesn't lose the session.
let memoryToken = sessionStorage.getItem(TOKEN_KEY) || null;

export function getToken() {
  return memoryToken;
}

export function getStoredUser() {
  const raw = sessionStorage.getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
}

export function setSession(token, user) {
  memoryToken = token;
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token);
  } else {
    sessionStorage.removeItem(TOKEN_KEY);
  }
  if (user) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    sessionStorage.removeItem(USER_KEY);
  }
}

export function clearSession() {
  memoryToken = null;
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function request(path, { method = 'GET', body, headers } = {}) {
  const token = getToken();
  const finalHeaders = { ...headers };
  if (body !== undefined) {
    finalHeaders['Content-Type'] = 'application/json';
  }
  if (token) {
    finalHeaders['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    method,
    headers: finalHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  // A 401 while we were carrying a token means the session is no longer valid
  // (expired/invalid JWT) -> force a fresh login. A 401 with no token (e.g. a
  // failed login attempt itself) is a normal error the caller must display.
  if (response.status === 401 && token) {
    clearSession();
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    throw new ApiError('Session expirée, veuillez vous reconnecter.', 401, null);
  }

  const text = await response.text();
  let parsed = null;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = text;
    }
  }

  if (!response.ok) {
    const message =
      (parsed && typeof parsed === 'object' && parsed.message) ||
      (typeof parsed === 'string' && parsed) ||
      `Erreur ${response.status}`;
    throw new ApiError(message, response.status, parsed);
  }

  return parsed;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: body ?? {} }),
  postNoBody: (path) => request(path, { method: 'POST' }),
  put: (path, body) => request(path, { method: 'PUT', body: body ?? {} }),
  // Sans corps : les actions d'administration (désactiver/réactiver) portent tout
  // dans l'URL. Le backend autorise explicitement PATCH côté CORS.
  patch: (path) => request(path, { method: 'PATCH' }),
  del: (path) => request(path, { method: 'DELETE' }),
};

export { ApiError };