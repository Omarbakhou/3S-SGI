const STYLES = {
  EN_ATTENTE: 'badge badge-pending',
  VALIDEE: 'badge badge-approved',
  REJETEE: 'badge badge-rejected',
  ANNULEE: 'badge badge-cancelled',
};

const LABELS = {
  EN_ATTENTE: 'En attente',
  VALIDEE: 'Validée',
  REJETEE: 'Rejetée',
  ANNULEE: 'Annulée',
};

export default function StatusBadge({ status }) {
  return <span className={STYLES[status] || 'badge'}>{LABELS[status] || status}</span>;
}
