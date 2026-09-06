import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth/AuthContext.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import ErrorBanner from '../components/ErrorBanner.jsx';

const today = () => new Date().toISOString().slice(0, 10);

export default function AdminPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState('projets');

  const [clients, setClients] = useState([]);
  const [projets, setProjets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    Promise.all([api.get('/api/clients'), api.get('/api/projets')])
      .then(([clientsData, projetsData]) => {
        setClients(clientsData);
        setProjets(projetsData);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  // ---- Comptes existants (employés + managers), partagé entre les deux onglets ----
  const [collaborateurs, setCollaborateurs] = useState([]);
  const [collabLoading, setCollabLoading] = useState(true);
  const [collabError, setCollabError] = useState(null);

  function loadCollaborateurs() {
    setCollabLoading(true);
    setCollabError(null);
    return Promise.all([api.get('/api/collaborateurs/employes'), api.get('/api/collaborateurs/managers')])
      .then(([employes, managers]) => {
        setCollaborateurs([
          ...employes.map((e) => ({ ...e, type: 'Employé' })),
          ...managers.map((m) => ({ ...m, type: 'Manager' })),
        ]);
      })
      .catch((err) => setCollabError(err.message))
      .finally(() => setCollabLoading(false));
  }

  useEffect(() => {
    loadCollaborateurs();
  }, []);

  const managers = collaborateurs.filter((c) => c.type === 'Manager');

  // ---- Activer / désactiver un compte ----
  // `demande` porte le compte visé tant que la modale est ouverte ; null = fermée.
  const [demande, setDemande] = useState(null);
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  async function confirmerChangementStatut() {
    if (!demande) return;
    const { id, type, actif, nomComplet } = demande;
    setActionBusy(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const verbe = actif ? 'desactiver' : 'reactiver';
      const misAJour = await api.patch(`/api/admin/comptes/${id}/${verbe}`);
      setCollaborateurs((list) =>
        list.map((c) => (c.id === id && c.type === type ? { ...c, actif: misAJour.actif } : c))
      );
      setActionSuccess(
        misAJour.actif ? `Compte « ${nomComplet} » réactivé.` : `Compte « ${nomComplet} » désactivé.`
      );
      setDemande(null);
    } catch (err) {
      // L'erreur reste affichée après fermeture de la modale : les refus métier
      // (dernier admin, propre compte) doivent être lisibles dans le tableau.
      setActionError(err.message);
      setDemande(null);
    } finally {
      setActionBusy(false);
    }
  }

  // ---- Créer un client ----
  const [clientNom, setClientNom] = useState('');
  const [clientBusy, setClientBusy] = useState(false);
  const [clientError, setClientError] = useState(null);

  async function handleCreateClient(e) {
    e.preventDefault();
    setClientError(null);
    setClientBusy(true);
    try {
      const created = await api.post('/api/clients', { nomClient: clientNom });
      setClients((list) => [...list, created]);
      setClientNom('');
    } catch (err) {
      setClientError(err.message);
    } finally {
      setClientBusy(false);
    }
  }

  // ---- Créer un projet ----
  const [projetForm, setProjetForm] = useState({ nom: '', clientId: '', dateDebut: today(), dateFin: '' });
  const [projetBusy, setProjetBusy] = useState(false);
  const [projetError, setProjetError] = useState(null);
  const [projetSuccess, setProjetSuccess] = useState(null);

  function updateProjetForm(field, value) {
    setProjetForm((f) => ({ ...f, [field]: value }));
  }

  async function handleCreateProjet(e) {
    e.preventDefault();
    setProjetError(null);
    setProjetSuccess(null);
    setProjetBusy(true);
    try {
      const created = await api.post('/api/projets', {
        nom: projetForm.nom,
        dateDebut: projetForm.dateDebut || null,
        dateFin: projetForm.dateFin || null,
        client: { id: Number(projetForm.clientId) },
      });
      // La réponse de création ne réhydrate pas toujours nomClient sur l'association :
      // on complète depuis la liste des clients déjà chargée pour un affichage immédiat correct.
      const clientComplet = clients.find((c) => c.id === Number(projetForm.clientId));
      setProjets((list) => [...list, { ...created, client: clientComplet || created.client }]);
      setProjetForm({ nom: '', clientId: projetForm.clientId, dateDebut: today(), dateFin: '' });
      setProjetSuccess(`Projet « ${created.nom} » créé.`);
    } catch (err) {
      setProjetError(err.message);
    } finally {
      setProjetBusy(false);
    }
  }

  // ---- Affecter un collaborateur à un projet ----
  const [affectationForm, setAffectationForm] = useState({ collaborateurId: '', projetId: '', taux: '', date: today() });
  const [affectationBusy, setAffectationBusy] = useState(false);
  const [affectationError, setAffectationError] = useState(null);
  const [affectationSuccess, setAffectationSuccess] = useState(null);

  function updateAffectationForm(field, value) {
    setAffectationForm((f) => ({ ...f, [field]: value }));
  }

  async function handleCreateAffectation(e) {
    e.preventDefault();
    setAffectationError(null);
    setAffectationSuccess(null);
    setAffectationBusy(true);
    try {
      await api.post('/api/affectations', {
        collaborateurId: Number(affectationForm.collaborateurId),
        projetId: Number(affectationForm.projetId),
        tauxAffectation: Number(affectationForm.taux),
        dateAffectation: affectationForm.date || null,
      });
      setAffectationSuccess('Affectation créée.');
      setAffectationForm((f) => ({ ...f, taux: '' }));
    } catch (err) {
      setAffectationError(err.message);
    } finally {
      setAffectationBusy(false);
    }
  }

  // ---- Créer un employé ----
  const [employeForm, setEmployeForm] = useState({ nom: '', prenom: '', email: '', motDePasse: '', managerId: '' });
  const [employeBusy, setEmployeBusy] = useState(false);
  const [employeError, setEmployeError] = useState(null);
  const [employeSuccess, setEmployeSuccess] = useState(null);

  function updateEmployeForm(field, value) {
    setEmployeForm((f) => ({ ...f, [field]: value }));
  }

  async function handleCreateEmploye(e) {
    e.preventDefault();
    setEmployeError(null);
    setEmployeSuccess(null);
    setEmployeBusy(true);
    try {
      const created = await api.post('/api/collaborateurs/employe', {
        nom: employeForm.nom,
        prenom: employeForm.prenom,
        email: employeForm.email,
        motDePasse: employeForm.motDePasse,
        manager: employeForm.managerId ? { id: Number(employeForm.managerId) } : null,
      });
      const managerComplet = managers.find((m) => m.id === Number(employeForm.managerId));
      setCollaborateurs((list) => [
        ...list,
        { ...created, manager: managerComplet || created.manager, type: 'Employé' },
      ]);
      setEmployeForm({ nom: '', prenom: '', email: '', motDePasse: '', managerId: employeForm.managerId });
      setEmployeSuccess(`Employé « ${created.nomComplet} » créé.`);
    } catch (err) {
      setEmployeError(err.message);
    } finally {
      setEmployeBusy(false);
    }
  }

  // ---- Créer un manager ----
  const [managerForm, setManagerForm] = useState({ nom: '', prenom: '', email: '', motDePasse: '' });
  const [managerBusy, setManagerBusy] = useState(false);
  const [managerError, setManagerError] = useState(null);
  const [managerSuccess, setManagerSuccess] = useState(null);

  function updateManagerForm(field, value) {
    setManagerForm((f) => ({ ...f, [field]: value }));
  }

  async function handleCreateManager(e) {
    e.preventDefault();
    setManagerError(null);
    setManagerSuccess(null);
    setManagerBusy(true);
    try {
      const created = await api.post('/api/collaborateurs/manager', {
        nom: managerForm.nom,
        prenom: managerForm.prenom,
        email: managerForm.email,
        motDePasse: managerForm.motDePasse,
      });
      setCollaborateurs((list) => [...list, { ...created, type: 'Manager' }]);
      setManagerForm({ nom: '', prenom: '', email: '', motDePasse: '' });
      setManagerSuccess(`Manager « ${created.nomComplet} » créé.`);
    } catch (err) {
      setManagerError(err.message);
    } finally {
      setManagerBusy(false);
    }
  }

  return (
    <div className="page">
      <h1>Administration</h1>

      <ConfirmDialog
        open={demande !== null}
        danger={demande?.actif === true}
        busy={actionBusy}
        titre={demande?.actif ? 'Désactiver ce compte ?' : 'Réactiver ce compte ?'}
        message={
          demande?.actif
            ? `${demande?.nomComplet} ne pourra plus se connecter. Le compte n'est pas supprimé : `
              + `ses imputations et ses absences restent conservées, et vous pourrez le réactiver.`
            : `${demande?.nomComplet} pourra de nouveau se connecter avec ses identifiants habituels.`
        }
        libelleConfirmer={demande?.actif ? 'Désactiver' : 'Réactiver'}
        onConfirm={confirmerChangementStatut}
        onCancel={() => setDemande(null)}
      />

      <div className="tabs">
        <button type="button" className={tab === 'projets' ? 'tab active' : 'tab'} onClick={() => setTab('projets')}>
          Projets
        </button>
        <button type="button" className={tab === 'comptes' ? 'tab active' : 'tab'} onClick={() => setTab('comptes')}>
          Comptes
        </button>
      </div>

      {tab === 'projets' && (
        <div className="tab-panel">
          <section className="card">
            <h2>Créer un client</h2>
            <ErrorBanner message={clientError} />
            <form onSubmit={handleCreateClient} className="form-grid">
              <label htmlFor="clientNom">Nom</label>
              <input
                id="clientNom"
                type="text"
                value={clientNom}
                onChange={(e) => setClientNom(e.target.value)}
                placeholder="Ex : Manufacture Girard"
                required
              />
              <button type="submit" className="btn btn-primary" disabled={clientBusy}>
                {clientBusy ? 'Création…' : 'Créer le client'}
              </button>
            </form>
          </section>

          <section className="card">
            <h2>Créer un projet</h2>
            {loading && <p className="muted">Chargement…</p>}
            <ErrorBanner message={error} />
            {!loading && !error && clients.length === 0 && (
              <p className="muted">Créez d'abord un client ci-dessus.</p>
            )}
            {!loading && !error && clients.length > 0 && (
              <>
                <ErrorBanner message={projetError} />
                {projetSuccess && <div className="success-banner">{projetSuccess}</div>}
                <form onSubmit={handleCreateProjet} className="form-grid">
                  <label htmlFor="projetNom">Nom</label>
                  <input
                    id="projetNom"
                    type="text"
                    value={projetForm.nom}
                    onChange={(e) => updateProjetForm('nom', e.target.value)}
                    placeholder="Ex : Refonte du site vitrine"
                    required
                  />

                  <label htmlFor="projetClient">Client</label>
                  <select
                    id="projetClient"
                    value={projetForm.clientId}
                    onChange={(e) => updateProjetForm('clientId', e.target.value)}
                    required
                  >
                    <option value="" disabled>
                      — Choisir —
                    </option>
                    {clients.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nomClient}
                      </option>
                    ))}
                  </select>

                  <label htmlFor="projetDateDebut">Date de début</label>
                  <input
                    id="projetDateDebut"
                    type="date"
                    value={projetForm.dateDebut}
                    onChange={(e) => updateProjetForm('dateDebut', e.target.value)}
                  />

                  <label htmlFor="projetDateFin">Date de fin</label>
                  <input
                    id="projetDateFin"
                    type="date"
                    value={projetForm.dateFin}
                    onChange={(e) => updateProjetForm('dateFin', e.target.value)}
                  />

                  <button type="submit" className="btn btn-primary" disabled={projetBusy}>
                    {projetBusy ? 'Création…' : 'Créer le projet'}
                  </button>
                </form>
              </>
            )}
          </section>

          <section className="card">
            <h2>Affecter un collaborateur à un projet</h2>
            {collabLoading && <p className="muted">Chargement…</p>}
            <ErrorBanner message={collabError} />
            {!collabLoading && !collabError && (
              <>
                <ErrorBanner message={affectationError} />
                {affectationSuccess && <div className="success-banner">{affectationSuccess}</div>}
                <form onSubmit={handleCreateAffectation} className="form-grid">
                  <label htmlFor="affectationCollaborateur">Collaborateur</label>
                  <select
                    id="affectationCollaborateur"
                    value={affectationForm.collaborateurId}
                    onChange={(e) => updateAffectationForm('collaborateurId', e.target.value)}
                    required
                  >
                    <option value="" disabled>
                      — Choisir —
                    </option>
                    {collaborateurs.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nomComplet} ({c.type})
                      </option>
                    ))}
                  </select>

                  <label htmlFor="affectationProjet">Projet</label>
                  <select
                    id="affectationProjet"
                    value={affectationForm.projetId}
                    onChange={(e) => updateAffectationForm('projetId', e.target.value)}
                    required
                  >
                    <option value="" disabled>
                      — Choisir —
                    </option>
                    {projets.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.nom}
                      </option>
                    ))}
                  </select>

                  <label htmlFor="affectationTaux">Taux (%)</label>
                  <input
                    id="affectationTaux"
                    type="number"
                    min="0"
                    max="100"
                    step="5"
                    value={affectationForm.taux}
                    onChange={(e) => updateAffectationForm('taux', e.target.value)}
                    required
                  />

                  <label htmlFor="affectationDate">Date</label>
                  <input
                    id="affectationDate"
                    type="date"
                    value={affectationForm.date}
                    onChange={(e) => updateAffectationForm('date', e.target.value)}
                  />

                  <button type="submit" className="btn btn-primary" disabled={affectationBusy}>
                    {affectationBusy ? 'Création…' : "Créer l'affectation"}
                  </button>
                </form>
              </>
            )}
          </section>

          <section className="card">
            <h2>Projets existants</h2>
            {!loading && !error && (
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
      )}

      {tab === 'comptes' && (
        <div className="tab-panel">
          <section className="card">
            <h2>Créer un employé</h2>
            <ErrorBanner message={employeError} />
            {employeSuccess && <div className="success-banner">{employeSuccess}</div>}
            <form onSubmit={handleCreateEmploye} className="form-grid">
              <label htmlFor="employeNom">Nom</label>
              <input
                id="employeNom"
                type="text"
                value={employeForm.nom}
                onChange={(e) => updateEmployeForm('nom', e.target.value)}
                required
              />

              <label htmlFor="employePrenom">Prénom</label>
              <input
                id="employePrenom"
                type="text"
                value={employeForm.prenom}
                onChange={(e) => updateEmployeForm('prenom', e.target.value)}
                required
              />

              <label htmlFor="employeEmail">Email</label>
              <input
                id="employeEmail"
                type="email"
                value={employeForm.email}
                onChange={(e) => updateEmployeForm('email', e.target.value)}
                required
              />

              <label htmlFor="employeMotDePasse">Mot de passe</label>
              <input
                id="employeMotDePasse"
                type="password"
                value={employeForm.motDePasse}
                onChange={(e) => updateEmployeForm('motDePasse', e.target.value)}
                required
              />

              <label htmlFor="employeManager">Manager (rattachement)</label>
              <select
                id="employeManager"
                value={employeForm.managerId}
                onChange={(e) => updateEmployeForm('managerId', e.target.value)}
              >
                <option value="">— Aucun —</option>
                {managers.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.nomComplet}
                  </option>
                ))}
              </select>

              <button type="submit" className="btn btn-primary" disabled={employeBusy}>
                {employeBusy ? 'Création…' : "Créer l'employé"}
              </button>
            </form>
          </section>

          <section className="card">
            <h2>Créer un manager</h2>
            <ErrorBanner message={managerError} />
            {managerSuccess && <div className="success-banner">{managerSuccess}</div>}
            <form onSubmit={handleCreateManager} className="form-grid">
              <label htmlFor="managerNom">Nom</label>
              <input
                id="managerNom"
                type="text"
                value={managerForm.nom}
                onChange={(e) => updateManagerForm('nom', e.target.value)}
                required
              />

              <label htmlFor="managerPrenom">Prénom</label>
              <input
                id="managerPrenom"
                type="text"
                value={managerForm.prenom}
                onChange={(e) => updateManagerForm('prenom', e.target.value)}
                required
              />

              <label htmlFor="managerEmail">Email</label>
              <input
                id="managerEmail"
                type="email"
                value={managerForm.email}
                onChange={(e) => updateManagerForm('email', e.target.value)}
                required
              />

              <label htmlFor="managerMotDePasse">Mot de passe</label>
              <input
                id="managerMotDePasse"
                type="password"
                value={managerForm.motDePasse}
                onChange={(e) => updateManagerForm('motDePasse', e.target.value)}
                required
              />

              <button type="submit" className="btn btn-primary" disabled={managerBusy}>
                {managerBusy ? 'Création…' : 'Créer le manager'}
              </button>
            </form>
          </section>

          <section className="card">
            <h2>Comptes existants</h2>
            {collabLoading && <p className="muted">Chargement…</p>}
            <ErrorBanner message={collabError} />
            <ErrorBanner message={actionError} />
            {actionSuccess && <div className="success-banner">{actionSuccess}</div>}
            {!collabLoading && !collabError && (
              <table className="table">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Email</th>
                    <th>Type</th>
                    <th>Rattaché à</th>
                    <th>Statut</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {collaborateurs.map((c) => {
                    const estMonCompte = c.id === user?.id;
                    return (
                      <tr key={`${c.type}-${c.id}`} className={c.actif ? undefined : 'ligne-inactive'}>
                        <td>{c.nomComplet}</td>
                        <td>{c.email}</td>
                        <td>{c.type}</td>
                        <td>{c.type === 'Employé' ? c.manager?.nomComplet || '—' : '—'}</td>
                        <td>
                          <span className={c.actif ? 'badge badge-actif' : 'badge badge-inactif'}>
                            {c.actif ? 'Actif' : 'Inactif'}
                          </span>
                        </td>
                        <td className="cellule-actions">
                          {estMonCompte ? (
                            <span className="muted">Votre compte</span>
                          ) : (
                            <button
                              type="button"
                              className={c.actif ? 'btn btn-danger' : 'btn btn-secondary'}
                              onClick={() => {
                                setActionError(null);
                                setActionSuccess(null);
                                setDemande(c);
                              }}
                            >
                              {c.actif ? 'Désactiver' : 'Réactiver'}
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
