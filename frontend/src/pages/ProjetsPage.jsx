import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const today = () => new Date().toISOString().slice(0, 10);
const EMPTY_FORM = { projetId: '', dateImputation: today(), heures: '', nom: '' };

export default function ProjetsPage() {
  const { user, hasRole } = useAuth();

  const [projets, setProjets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [submitSuccess, setSubmitSuccess] = useState(null);

  const [historique, setHistorique] = useState([]);
  const [historiqueLoading, setHistoriqueLoading] = useState(hasRole('EMPLOYE'));
  const [historiqueError, setHistoriqueError] = useState(null);

  const [cumuls, setCumuls] = useState({});
  const [cumulsLoading, setCumulsLoading] = useState(hasRole('MANAGER', 'ADMIN'));
  const [cumulsError, setCumulsError] = useState(null);

  function loadHistorique() {
    setHistoriqueLoading(true);
    setHistoriqueError(null);
    api
      .get(`/api/imputations/employe/${user.id}`)
      .then((data) => {
        const tri = [...data].sort((a, b) => (b.dateImputation || '').localeCompare(a.dateImputation || ''));
        setHistorique(tri);
      })
      .catch((err) => setHistoriqueError(err.message))
      .finally(() => setHistoriqueLoading(false));
  }

  useEffect(() => {
    setLoading(true);
    setError(null);

    // GET /api/projets (la liste complète) est réservé MANAGER/ADMIN côté backend.
    // Un simple EMPLOYE ne voit que les projets auxquels il est affecté.
    const request = hasRole('MANAGER', 'ADMIN')
      ? api.get('/api/projets')
      : api.get(`/api/affectations/collaborateur/${user.id}`).then((affectations) => {
          const byId = new Map();
          affectations.forEach((a) => {
            if (a.projet) byId.set(a.projet.id, a.projet);
          });
          return [...byId.values()];
        });

    request
      .then((data) => {
        setProjets(data);
        if (data.length > 0) {
          setForm((f) => ({ ...f, projetId: String(data[0].id) }));
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));

    if (hasRole('EMPLOYE')) {
      loadHistorique();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!hasRole('MANAGER', 'ADMIN') || projets.length === 0) {
      setCumulsLoading(false);
      return;
    }
    setCumulsLoading(true);
    setCumulsError(null);
    Promise.all(
      projets.map((p) =>
        api.get(`/api/imputations/projet/${p.id}/cumul-heures`).then((total) => [p.id, total])
      )
    )
      .then((pairs) => setCumuls(Object.fromEntries(pairs)))
      .catch((err) => setCumulsError(err.message))
      .finally(() => setCumulsLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projets]);

  function handleFieldChange(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitError(null);
    setSubmitSuccess(null);
    setSubmitting(true);
    try {
      const created = await api.post(`/api/imputations/employe/${user.id}`, {
        projetId: Number(form.projetId),
        dateImputation: form.dateImputation,
        heures: Number(form.heures),
        nom: form.nom,
      });
      setHistorique((list) => [created, ...list]);
      setForm((f) => ({ ...EMPTY_FORM, projetId: f.projetId }));
      setSubmitSuccess('Imputation enregistrée, en attente de validation.');
    } catch (err) {
      // Règles métier backend (heures invalides, projet non affecté, doublon, date
      // trop lointaine) : le message est affiché tel quel, il est déjà explicite.
      setSubmitError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h1>Imputations</h1>

      {hasRole('EMPLOYE') && (
        <section className="card">
          <h2>Saisir une imputation</h2>
          {loading && <p className="muted">Chargement des projets…</p>}
          {!loading && projets.length === 0 && (
            <p className="muted">Aucun projet auquel imputer du temps pour le moment.</p>
          )}
          {!loading && projets.length > 0 && (
            <>
              <ErrorBanner message={submitError} />
              {submitSuccess && <div className="success-banner">{submitSuccess}</div>}
              <form onSubmit={handleSubmit} className="form-grid">
                <label htmlFor="projet">Projet</label>
                <select id="projet" value={form.projetId} onChange={(e) => handleFieldChange('projetId', e.target.value)}>
                  {projets.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nom}
                    </option>
                  ))}
                </select>

                <label htmlFor="dateImputation">Date</label>
                <input
                  id="dateImputation"
                  type="date"
                  value={form.dateImputation}
                  onChange={(e) => handleFieldChange('dateImputation', e.target.value)}
                  required
                />

                <label htmlFor="heures">Heures</label>
                <input
                  id="heures"
                  type="number"
                  step="0.5"
                  min="0.5"
                  max="24"
                  value={form.heures}
                  onChange={(e) => handleFieldChange('heures', e.target.value)}
                  required
                />

                <label htmlFor="nom">Description</label>
                <input
                  id="nom"
                  type="text"
                  value={form.nom}
                  onChange={(e) => handleFieldChange('nom', e.target.value)}
                  placeholder="Ex : Développement module facturation"
                  required
                />

                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Envoi…' : "Enregistrer l'imputation"}
                </button>
              </form>
            </>
          )}
        </section>
      )}

      {hasRole('EMPLOYE') && (
        <section className="card">
          <h2>Mon historique d'imputations</h2>
          {historiqueLoading && <p className="muted">Chargement…</p>}
          <ErrorBanner message={historiqueError} />
          {!historiqueLoading && !historiqueError && historique.length === 0 && (
            <p className="muted">Aucune imputation enregistrée.</p>
          )}
          {!historiqueLoading && !historiqueError && historique.length > 0 && (
            <table className="table">
              <thead>
                <tr>
                  <th>Projet</th>
                  <th>Date</th>
                  <th>Heures</th>
                  <th>Description</th>
                  <th>Statut</th>
                  <th>Motif de rejet</th>
                </tr>
              </thead>
              <tbody>
                {historique.map((i) => (
                  <tr key={i.id}>
                    <td>{i.projetNom}</td>
                    <td>{i.dateImputation || '—'}</td>
                    <td>{i.heures ?? '—'}</td>
                    <td>{i.nom}</td>
                    <td>
                      <StatusBadge status={i.statut} />
                    </td>
                    <td>{i.motifRejet || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}

      {hasRole('MANAGER', 'ADMIN') && (
        <section className="card">
          <h2>Cumul d'heures validées par projet</h2>
          {cumulsLoading && <p className="muted">Chargement…</p>}
          <ErrorBanner message={cumulsError} />
          {!cumulsLoading && !cumulsError && projets.length > 0 && (
            <table className="table">
              <thead>
                <tr>
                  <th>Projet</th>
                  <th>Client</th>
                  <th>Heures validées</th>
                </tr>
              </thead>
              <tbody>
                {projets.map((p) => (
                  <tr key={p.id}>
                    <td>{p.nom}</td>
                    <td>{p.client?.nomClient || '—'}</td>
                    <td>{cumuls[p.id] ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}

      <section className="card">
        <h2>Liste des projets</h2>
        {loading && <p className="muted">Chargement…</p>}
        <ErrorBanner message={error} />
        {!loading && !error && projets.length === 0 && <p className="muted">Aucun projet visible.</p>}
        {!loading && !error && projets.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Client</th>
                <th>Début</th>
                <th>Fin</th>
              </tr>
            </thead>
            <tbody>
              {projets.map((p) => (
                <tr key={p.id}>
                  <td>{p.nom}</td>
                  <td>{p.client?.nomClient || '—'}</td>
                  <td>{p.dateDebut || '—'}</td>
                  <td>{p.dateFin || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
