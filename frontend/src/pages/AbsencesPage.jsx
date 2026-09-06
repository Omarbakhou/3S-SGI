


import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const TYPES_ABSENCE = [
  'CONGE_PAYE',
  'RTT',
  'CONGE_EXCEPTIONNEL',
  'MALADIE',
  'CONGE_MATERNITE_PATERNITE',
  'SANS_SOLDE',
];

const EMPTY_FORM = { typeAbsence: TYPES_ABSENCE[0], dateDebut: '', dateFin: '', commentaireEmploye: '' };

export default function AbsencesPage() {
  const { user, hasRole } = useAuth();

  const [absences, setAbsences] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [submitSuccess, setSubmitSuccess] = useState(null);

  function loadAbsences() {
    setLoading(true);
    setError(null);
    api
      .get(`/api/absences/employe/${user.id}`)
      .then((data) => {
        data.sort((a, b) => (b.dateDemande || '').localeCompare(a.dateDemande || ''));
        setAbsences(data);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  // Un ADMIN est aussi un MANAGER (email listé dans ADMIN_EMAILS) : il faut l'exclure
  // explicitement. Un ADMIN ne dépose pas d'absence, il n'a pas d'approbateur — il valide
  // celles des managers (voir ValidationPage).
  const peutDeposer = !hasRole('ADMIN') && hasRole('EMPLOYE', 'MANAGER');

  useEffect(() => {
    if (peutDeposer) {
      loadAbsences();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!peutDeposer) {
    return (
      <div className="page">
        <h1>Mes absences</h1>
        <p className="muted">
          {hasRole('ADMIN')
            ? "Un administrateur ne dépose pas de demande d'absence : il valide celles des managers depuis l'onglet Validation."
            : 'Cette page est réservée aux comptes employé et manager.'}
        </p>
      </div>
    );
  }

  function handleFieldChange(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitError(null);
    setSubmitSuccess(null);
    setSubmitting(true);
    try {
      const created = await api.post(`/api/absences/employe/${user.id}`, form);
      setAbsences((list) => [created, ...list]);
      setForm(EMPTY_FORM);
      setSubmitSuccess('Demande envoyée, en attente de validation.');
    } catch (err) {
      // Backend validation errors (quota insuffisant, chevauchement de dates, ...)
      // are shown exactly as returned so the user knows why the request failed.
      setSubmitError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h1>Mes absences</h1>

      <section className="card">
        <h2>Nouvelle demande</h2>
        <ErrorBanner message={submitError} />
        {submitSuccess && <div className="success-banner">{submitSuccess}</div>}
        <form onSubmit={handleSubmit} className="form-grid">
          <label htmlFor="typeAbsence">Type</label>
          <select
            id="typeAbsence"
            value={form.typeAbsence}
            onChange={(e) => handleFieldChange('typeAbsence', e.target.value)}
          >
            {TYPES_ABSENCE.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>

          <label htmlFor="dateDebut">Date de début</label>
          <input
            id="dateDebut"
            type="date"
            value={form.dateDebut}
            onChange={(e) => handleFieldChange('dateDebut', e.target.value)}
            required
          />

          <label htmlFor="dateFin">Date de fin</label>
          <input
            id="dateFin"
            type="date"
            value={form.dateFin}
            onChange={(e) => handleFieldChange('dateFin', e.target.value)}
            required
          />

          <label htmlFor="commentaire">Commentaire</label>
          <textarea
            id="commentaire"
            value={form.commentaireEmploye}
            onChange={(e) => handleFieldChange('commentaireEmploye', e.target.value)}
            rows={2}
          />

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Envoi…' : 'Envoyer la demande'}
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Historique</h2>
        {loading && <p className="muted">Chargement…</p>}
        <ErrorBanner message={error} />
        {!loading && !error && absences.length === 0 && <p className="muted">Aucune absence enregistrée.</p>}
        {!loading && !error && absences.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Début</th>
                <th>Fin</th>
                <th>Jours</th>
                <th>Statut</th>
                <th>Commentaire</th>
                <th>Motif de rejet</th>
              </tr>
            </thead>
            <tbody>
              {absences.map((a) => (
                <tr key={a.id}>
                  <td>{a.typeAbsence}</td>
                  <td>{a.dateDebut}</td>
                  <td>{a.dateFin}</td>
                  <td>{a.nombreJours}</td>
                  <td>
                    <StatusBadge status={a.statut} />
                  </td>
                  <td>{a.commentaireEmploye || '—'}</td>
                  <td>{a.motifRejet || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
