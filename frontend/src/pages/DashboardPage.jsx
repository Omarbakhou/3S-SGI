import { useEffect, useState } from 'react';
import { useAuth, displayName } from '../auth/AuthContext.jsx';
import { api } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const CURRENT_YEAR = new Date().getFullYear();

function Section({ title, loading, error, children, primary }) {
  return (
    <section className={primary ? 'card card-primary' : 'card card-secondary'}>
      <h2>{title}</h2>
      {loading && <p className="muted">Chargement…</p>}
      <ErrorBanner message={error} />
      {!loading && !error && children}
    </section>
  );
}

/**
 * EMPLOYE et MANAGER sont des types de compte mutuellement exclusifs dans ce backend
 * (Collaborateur est soit un Employe, soit un Manager) ; ADMIN est un MANAGER en plus
 * listé dans ADMIN_EMAILS. Chaque section est donc indépendamment conditionnée par rôle.
 *
 * L'imputation est le module central du système : ses sections sont toujours affichées
 * en premier (card-primary) ; les absences passent en second plan (card-secondary).
 */
export default function DashboardPage() {
  const { user, hasRole } = useAuth();

  // Un ADMIN est aussi un MANAGER (email listé dans ADMIN_EMAILS) : il faut l'exclure
  // explicitement. Un ADMIN ne dépose pas d'absence, il valide celles des managers.
  const peutDeposerAbsence = !hasRole('ADMIN') && hasRole('EMPLOYE', 'MANAGER');

  const [mesImputations, setMesImputations] = useState({ loading: hasRole('EMPLOYE'), error: null, data: [] });
  const [impEnAttente, setImpEnAttente] = useState({ loading: hasRole('MANAGER', 'ADMIN'), error: null, data: [] });
  const [cumulProjets, setCumulProjets] = useState({ loading: hasRole('ADMIN'), error: null, rows: [] });

  const [quotas, setQuotas] = useState({ loading: peutDeposerAbsence, error: null, data: [] });
  const [absences, setAbsences] = useState({ loading: peutDeposerAbsence, error: null, data: [] });
  const [absEnAttente, setAbsEnAttente] = useState({ loading: hasRole('MANAGER', 'ADMIN'), error: null, data: [] });
  const [totals, setTotals] = useState({ loading: hasRole('ADMIN'), error: null, employes: 0, managers: 0 });

  useEffect(() => {
    if (hasRole('EMPLOYE')) {
      api
        .get(`/api/imputations/employe/${user.id}`)
        .then((data) => {
          const recent = [...data]
            .sort((a, b) => (b.dateImputation || '').localeCompare(a.dateImputation || ''))
            .slice(0, 5);
          setMesImputations({ loading: false, error: null, data: recent });
        })
        .catch((err) => setMesImputations({ loading: false, error: err.message, data: [] }));
    }

    // Un manager (non-admin) peut aussi déposer ses propres demandes d'absence (voir
    // AbsencesPage) : ces deux cartes s'affichent donc pour EMPLOYE comme pour MANAGER,
    // mais jamais pour ADMIN (qui valide, ne dépose pas).
    if (peutDeposerAbsence) {
      api
        .get(`/api/absences/quotas/employe/${user.id}/annee/${CURRENT_YEAR}`)
        .then((data) => setQuotas({ loading: false, error: null, data }))
        .catch((err) => setQuotas({ loading: false, error: err.message, data: [] }));

      api
        .get(`/api/absences/employe/${user.id}`)
        .then((data) => {
          const recent = [...data]
            .sort((a, b) => (b.dateDemande || '').localeCompare(a.dateDemande || ''))
            .slice(0, 5);
          setAbsences({ loading: false, error: null, data: recent });
        })
        .catch((err) => setAbsences({ loading: false, error: err.message, data: [] }));
    }

    if (hasRole('MANAGER', 'ADMIN')) {
      api
        .get(`/api/imputations/en-attente?managerId=${user.id}`)
        .then((data) => setImpEnAttente({ loading: false, error: null, data }))
        .catch((err) => setImpEnAttente({ loading: false, error: err.message, data: [] }));

      api
        .get('/api/absences/en-attente')
        .then((data) => setAbsEnAttente({ loading: false, error: null, data }))
        .catch((err) => setAbsEnAttente({ loading: false, error: err.message, data: [] }));
    }

    if (hasRole('ADMIN')) {
      api
        .get('/api/projets')
        .then((projets) =>
          Promise.all(
            projets.map((p) =>
              api.get(`/api/imputations/projet/${p.id}/cumul-heures`).then((total) => ({
                id: p.id,
                nom: p.nom,
                total,
              }))
            )
          )
        )
        .then((rows) => setCumulProjets({ loading: false, error: null, rows }))
        .catch((err) => setCumulProjets({ loading: false, error: err.message, rows: [] }));

      Promise.all([api.get('/api/collaborateurs/employes'), api.get('/api/collaborateurs/managers')])
        .then(([employes, managers]) =>
          setTotals({ loading: false, error: null, employes: employes.length, managers: managers.length })
        )
        .catch((err) => setTotals((t) => ({ ...t, loading: false, error: err.message })));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="dashboard">
      <h1>Bonjour, {displayName(user)}</h1>

      {hasRole('EMPLOYE') && (
        <div className="dashboard-grid">
          <Section title="Mes imputations récentes" loading={mesImputations.loading} error={mesImputations.error} primary>
            {mesImputations.data.length === 0 ? (
              <p className="muted">Aucune imputation enregistrée.</p>
            ) : (
              <ul className="list">
                {mesImputations.data.map((i) => (
                  <li key={i.id}>
                    <strong>{i.projetNom}</strong> — {i.dateImputation} · {i.heures} h <StatusBadge status={i.statut} />
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>
      )}

      {hasRole('MANAGER', 'ADMIN') && (
        <div className="dashboard-grid">
          <Section title="Imputations en attente de validation" loading={impEnAttente.loading} error={impEnAttente.error} primary>
            <p className="stat">{impEnAttente.data.length}</p>
            <p className="muted">imputation(s) à valider.</p>
          </Section>

          {hasRole('ADMIN') && (
            <Section title="Cumul d'heures par projet" loading={cumulProjets.loading} error={cumulProjets.error} primary>
              {cumulProjets.rows.length === 0 ? (
                <p className="muted">Aucun projet.</p>
              ) : (
                <table className="table">
                  <thead>
                    <tr>
                      <th>Projet</th>
                      <th>Heures validées</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cumulProjets.rows.map((r) => (
                      <tr key={r.id}>
                        <td>{r.nom}</td>
                        <td>{r.total}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </Section>
          )}
        </div>
      )}

      {peutDeposerAbsence && (
        <div className="dashboard-grid">
          <Section title="Mon quota d'absences" loading={quotas.loading} error={quotas.error}>
            {quotas.data.length === 0 ? (
              <p className="muted">Aucun quota alloué pour {CURRENT_YEAR}.</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Alloués</th>
                    <th>Pris</th>
                    <th>Restants</th>
                  </tr>
                </thead>
                <tbody>
                  {quotas.data.map((q) => (
                    <tr key={q.id}>
                      <td>{q.typeAbsence}</td>
                      <td>{q.joursAlloues}</td>
                      <td>{q.joursPris}</td>
                      <td>{q.joursRestants}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Section>

          <Section title="Absences récentes" loading={absences.loading} error={absences.error}>
            {absences.data.length === 0 ? (
              <p className="muted">Aucune absence enregistrée.</p>
            ) : (
              <ul className="list">
                {absences.data.map((a) => (
                  <li key={a.id}>
                    <strong>{a.typeAbsence}</strong> — {a.dateDebut} → {a.dateFin}{' '}
                    <StatusBadge status={a.statut} />
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>
      )}

      {hasRole('MANAGER', 'ADMIN') && (
        <div className="dashboard-grid">
          <Section title="Demandes d'absence en attente" loading={absEnAttente.loading} error={absEnAttente.error}>
            <p className="stat">{absEnAttente.data.length}</p>
            <p className="muted">demande(s) d'absence à valider.</p>
          </Section>

          {hasRole('ADMIN') && (
            <Section title="Totaux (toute l'entreprise)" loading={totals.loading} error={totals.error}>
              <p>
                <strong>{totals.employes}</strong> employé(s)
              </p>
              <p>
                <strong>{totals.managers}</strong> manager(s)
              </p>
            </Section>
          )}
        </div>
      )}
    </div>
  );
}
