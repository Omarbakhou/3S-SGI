import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Le serveur de dev relaie /api vers Spring Boot : le navigateur ne parle qu'à
// une seule origine (pas de CORS en dev, le JWT reste transmis manuellement).
//
// `host: true` fait écouter Vite sur toutes les interfaces, sans quoi il ne
// répond qu'à 127.0.0.1 et l'entrée `sgi.local` du fichier hosts ne sert à rien.
// `allowedHosts` autorise explicitement le nom de domaine : Vite refuse par
// défaut les requêtes dont l'en-tête Host lui est inconnu (« Blocked request »).
//
// Rappel : `sgi.local` est une configuration locale par poste (fichier hosts,
// non versionnable), pas une configuration de déploiement. Voir le README.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    allowedHosts: ['sgi.local', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
});
