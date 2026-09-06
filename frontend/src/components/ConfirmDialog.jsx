import { useEffect, useRef } from 'react';

/**
 * Boîte de confirmation modale.
 *
 * Remplace window.confirm, qui bloque le thread du navigateur, ne peut pas être
 * mis aux couleurs de l'application et n'est pas pilotable dans un test.
 *
 * Accessibilité : le focus part sur le bouton d'annulation (l'action non
 * destructrice), Échap ferme, et le clic sur le fond ferme également.
 */
export default function ConfirmDialog({
  open,
  titre,
  message,
  libelleConfirmer = 'Confirmer',
  libelleAnnuler = 'Annuler',
  danger = false,
  busy = false,
  onConfirm,
  onCancel,
}) {
  const boutonAnnulerRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    boutonAnnulerRef.current?.focus();

    function onKeyDown(e) {
      if (e.key === 'Escape' && !busy) {
        onCancel();
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, busy, onCancel]);

  if (!open) return null;

  return (
    <div
      className="modal-overlay"
      onMouseDown={(e) => {
        // Ne ferme que sur un clic sur le fond lui-même, pas sur la boîte.
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="confirm-titre">
        <h2 id="confirm-titre" className="modal-titre">
          {titre}
        </h2>
        <p className="modal-message">{message}</p>
        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-secondary"
            ref={boutonAnnulerRef}
            onClick={onCancel}
            disabled={busy}
          >
            {libelleAnnuler}
          </button>
          <button
            type="button"
            className={danger ? 'btn btn-danger' : 'btn btn-primary'}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? 'En cours…' : libelleConfirmer}
          </button>
        </div>
      </div>
    </div>
  );
}
