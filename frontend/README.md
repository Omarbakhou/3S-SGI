# SGI — Frontend (demo)

Vite + React + React Router, plain CSS. Talks to the Spring Boot backend at
`http://localhost:8081` through the Vite dev proxy (`/api/*`).

## Run

1. Start the backend first (port 8081).
2. From this folder:
   ```
   npm install
   npm run dev
   ```
3. Open http://localhost:5173 and log in with any existing collaborateur
   account (email + `motDePasse`).

## Notes

- The JWT is kept in memory and in `sessionStorage` (cleared on logout or on
  any `401` response) — never persisted anywhere else.
- `/validation` requires the `MANAGER` or `ADMIN` role; an `EMPLOYE` account
  is redirected back to `/dashboard`.
- `/absences` (viewing/creating own absence requests) only applies to
  `EMPLOYE` accounts, since `MANAGER`/`ADMIN` rows have no employee record in
  the backend.
- The project list on `/projets` comes from `GET /api/projets` for
  `MANAGER`/`ADMIN`, and from the user's own affectations
  (`GET /api/affectations/collaborateur/{id}`) for `EMPLOYE`, since the
  backend restricts the full project list to managers.
