import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';

function useRowState() {
  const [rowState, setRowState] = useState({});
  function updateRow(id, patch) {
    setRowState((s) => ({ ...s, [id]: { ...s[id], ...patch } }));
  }
  return [rowState, updateRow];
}

export default function ValidationPage() {
  const { user, hasRole } = useAuth();
  // L'imputation est le module central du système : c'est l'onglet par défaut.
  const [tab, setTab] = useState('imputations');

  const [imputations, setImputations] = useState([]);
  const [impLoading, setImpLoading] = useState(true);
  const [impError, setImpError] = useState(null);
  const [impRowState, updateImpRow] = useRowState();

  const [absences, setAbsences] = useState([]);
  const [absLoading, setAbsLoading] = useState(true);
  const [absError, setAbsError] = useState(null);
  const [absRowState, updateAbsRow] = useRowState();

  useEffect(() => {
    api
      .get(`/api/imputations/en-attente?managerId=${user.id}`)
      .then(setImputations)
      .catch((err) => setImpError(err.message))
      .finally(() => setImpLoading(false));

    api
      .get('/api/absences/en-attente')
      .then(setAbsences)
      .catch((err) => setAbsError(err.message))
      .finally(() => setAbsLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleApproveImputation(id) {
    updateImpRow(id, { busy: true, error: null });
    try {
      await api.postNoBody(`/api/imputations/${id}/valider?managerId=${user.id}`);
      setImputations((list) => list.filter((i) => i.id !== id));
    } catch (err) {
      updateImpRow(id, { busy: false, error: err.message });
    }
  }

  async function handleRejectImputation(id) {
    const motif = (impRowState[id]?.motif || '').trim();
    if (!motif) {
      updateImpRow(id, { error: 'Un motif est requis pour rejeter la demande.' });
      return;
    }
    updateImpRow(id, { busy: true, error: null });
    try {
      await api.post(`/api/imputations/${id}/rejeter?managerId=${user.id}`, { motif });
      setImputations((list) => list.filter((i) => i.id !== id));
    } catch (err) {
      updateImpRow(id, { busy: false, error: err.message });
    }
  }

  async function handleApproveAbsence(id) {
    updateAbsRow(id, { busy: true, error: null });
    try {
      await api.postNoBody(`/api/absences/${id}/valider?managerId=${user.id}`);
      setAbsences((list) => list.filter((a) => a.id !== id));
    } catch (err) {
      updateAbsRow(id, { busy: false, error: err.message });
    }
  }

  async function handleRejectAbsence(id) {
    const motif = (absRowState[id]?.motif || '').trim();
    if (!motif) {
      updateAbsRow(id, { error: 'Un motif est requis pour rejeter la demande.' });
      return;
    }
    updateAbsRow(id, { busy: true, error: null });
    try {
      await api.post(`/api/absences/${id}/rejeter?managerId=${user.id}`, { motif });
      setAbsences((list) => list.filter((a) => a.id !== id));
    } catch (err) {
      updateAbsRow(id, { busy: false, error: err.message });
    }
  }

  return (
    <div className="page">
      <h1>Validation</h1>

      <div className="tabs">
        <button
          type="button"
          className={tab === 'imputations' ? 'tab active' : 'tab'}
          onClick={() => setTab('imputations')}
        >
          Imputations {imputations.length > 0 && <span className="tab-count">{imputations.length}</span>}
        </button>
        <button
          type="button"
          className={tab === 'absences' ? 'tab active' : 'tab'}
          onClick={() => setTab('absences')}
        >
          Absences {absences.length > 0 && <span className="tab-count">{absences.length}</span>}
        </button>
      </div>

      {tab === 'imputations' && (
        <div className="tab-panel">
          {impLoading && <p className="muted">Chargement…</p>}
          <ErrorBanner message={impError} />
          {!impLoading && !impError && imputations.length === 0 && (
            <p className="muted">Aucune imputation en attente.</p>
          )}
          {!impLoading && !impError && imputations.length > 0 && (
            <div className="validation-list">
              {imputations.map((i) => {
                const row = impRowState[i.id] || {};
                return (
                  <div className="card validation-item" key={i.id}>
                    <div className="validation-header">
                      <strong>{i.employeNomComplet}</strong>
                      <span>{i.projetNom}</span>
                      <span>
                        {i.dateImputation} — {i.heures} h
                      </span>
                    </div>
                    {i.nom && <p className="muted">« {i.nom} »</p>}

                    <ErrorBanner message={row.error} />

                    <div className="validation-actions">
                      <button
                        type="button"
                        className="btn btn-primary"
                        disabled={row.busy}
                        onClick={() => handleApproveImputation(i.id)}
                      >
                        Approuver
                      </button>
                      <input
                        type="text"
                        placeholder="Motif de rejet"
                        value={row.motif || ''}
                        onChange={(e) => updateImpRow(i.id, { motif: e.target.value })}
                        disabled={row.busy}
                      />
                      <button
                        type="button"
                        className="btn btn-danger"
                        disabled={row.busy}
                        onClick={() => handleRejectImputation(i.id)}
                      >
                        Rejeter
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {tab === 'absences' && (
        <div className="tab-panel">
          {absLoading && <p className="muted">Chargement…</p>}
          <ErrorBanner message={absError} />
          {!absLoading && !absError && absences.length === 0 && (
            <p className="muted">Aucune demande en attente.</p>
          )}
          {!absLoading && !absError && absences.length > 0 && (
            <div className="validation-list">
              {absences.map((a) => {
                const row = absRowState[a.id] || {};
                // La demande d'un manager ne peut être validée/rejetée que par un ADMIN :
                // un manager pair voit une note plutôt que des boutons qui échoueraient en 403.
                const reserveeAuxAdmins = a.collaborateurType === 'MANAGER' && !hasRole('ADMIN');
                return (
                  <div className="card validation-item" key={a.id}>
                    <div className="validation-header">
                      <strong>{a.employeNomComplet}</strong>
                      {a.collaborateurType === 'MANAGER' && <span className="badge">MANAGER</span>}
                      <span>{a.typeAbsence}</span>
                      <span>
                        {a.dateDebut} → {a.dateFin} ({a.nombreJours} j)
                      </span>
                    </div>
                    {a.commentaireEmploye && <p className="muted">« {a.commentaireEmploye} »</p>}

                    <ErrorBanner message={row.error} />

                    {reserveeAuxAdmins ? (
                      <p className="muted">Seul un administrateur peut valider ou rejeter la demande d'un manager.</p>
                    ) : (
                      <div className="validation-actions">
                        <button
                          type="button"
                          className="btn btn-primary"
                          disabled={row.busy}
                          onClick={() => handleApproveAbsence(a.id)}
                        >
                          Approuver
                        </button>
                        <input
                          type="text"
                          placeholder="Motif de rejet"
                          value={row.motif || ''}
                          onChange={(e) => updateAbsRow(a.id, { motif: e.target.value })}
                          disabled={row.busy}
                        />
                        <button
                          type="button"
                          className="btn btn-danger"
                          disabled={row.busy}
                          onClick={() => handleRejectAbsence(a.id)}
                        >
                          Rejeter
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
