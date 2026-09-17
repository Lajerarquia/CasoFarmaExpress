import { useMemo, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usePrescriptions } from '../../context/PrescriptionsContext';
import { apiErrorMessage } from '../../api/apiErrors';
import { isHttpUrl, PRESCRIPTION_STATUSES, NEXT_STATUS } from '../../api/contracts';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import Drawer from '../../components/Drawer/Drawer';
import styles from './Prescriptions.module.css';

const NEXT_LABEL = {
  INGRESADA: 'Validar',
  VALIDADA: 'Pasar a preparación',
  EN_PREPARACION: 'Marcar lista para retiro',
  LISTA_RETIRO: 'Marcar dispensada',
};

export default function Prescriptions() {
  const { user } = useAuth();
  const roles = user?.roles || [];
  const isOperador = roles.includes('Operador') || roles.includes('Admin');

  const { recetas, loading, usingMock, error, getReceta, updateStatus, refresh } = usePrescriptions();
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedReceta, setSelectedReceta] = useState(null);
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);

  const filtered = useMemo(
    () => (statusFilter ? recetas.filter((r) => r.status === statusFilter) : recetas),
    [recetas, statusFilter]
  );

  const handleSelect = async (id) => {
    if (busy) return;
    setActionError('');
    setBusy(true);
    try {
      setSelectedReceta(await getReceta(id));
    } catch (err) {
      setActionError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const handleStatus = async (receta, next) => {
    setActionError('');
    setBusy(true);
    try {
      await updateStatus(receta.id, next);
      setSelectedReceta(null);
    } catch (err) {
      setActionError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>Recetas</h1>
        <button onClick={refresh} disabled={loading || busy || Boolean(selectedReceta)}>Actualizar conexión</button>
      </div>

      {usingMock && (
        <p className={styles.mockNotice}>
          Modo de ejemplo: el servicio no está disponible. Los cambios se guardan solo en este navegador.
        </p>
      )}
      {(error || actionError) && <p role="alert" className={styles.error}>{actionError || error}</p>}
      {busy && <p role="status">Procesando receta...</p>}

      <div className={styles.filters}>
        <select
          className={styles.select}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">Todos los estados</option>
          {PRESCRIPTION_STATUSES.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>

      {loading && <p className={styles.state}>Cargando recetas...</p>}

      {!loading && (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Paciente / identificador</th>
              <th>Estado</th>
              <th>Fecha</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((receta) => (
              <tr key={receta.id} className={styles.row} onClick={() => handleSelect(receta.id)}>
                <td>{receta.pacienteNombre}</td>
                <td>
                  <StatusBadge status={receta.status} />
                </td>
                <td>{receta.fechaCreacion}</td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={3} className={styles.empty}>
                  No hay recetas para este filtro.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      <Drawer
        isOpen={!!selectedReceta}
        onClose={() => { if (!busy) setSelectedReceta(null); }}
        title={selectedReceta ? `Receta de ${selectedReceta.pacienteNombre}` : ''}
      >
        {selectedReceta && (
          <>
            <div>
              <StatusBadge status={selectedReceta.status} />
            </div>
            <p className={styles.drawerText}>Fecha: {selectedReceta.fechaCreacion}</p>
            <p className={styles.drawerText}>Farmacia: {selectedReceta.pharmacyId || 'Sin indicar'}</p>
            {isHttpUrl(selectedReceta.imageUrl) && (
              <a href={selectedReceta.imageUrl} target="_blank" rel="noopener noreferrer">Ver receta</a>
            )}
            {actionError && <p role="alert" className={styles.error}>{actionError}</p>}

            {isOperador && NEXT_STATUS[selectedReceta.status] && (
              <button className={styles.actionButton} disabled={busy}
                onClick={() => handleStatus(selectedReceta, NEXT_STATUS[selectedReceta.status])}>
                {NEXT_LABEL[selectedReceta.status]}
              </button>
            )}

            {isOperador && ['INGRESADA', 'VALIDADA'].includes(selectedReceta.status) && (
              <button className={styles.rejectButton} disabled={busy}
                onClick={() => handleStatus(selectedReceta, 'RECHAZADA')}>
                Rechazar
              </button>
            )}
          </>
        )}
      </Drawer>
    </div>
  );
}
