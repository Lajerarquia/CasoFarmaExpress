import { useState } from 'react';
import { apiErrorMessage } from '../../api/apiErrors';
import { usePrescriptions } from '../../context/PrescriptionsContext';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import styles from './Dashboard.module.css';

export default function Dashboard() {
  const { recetas, loading, usingMock, error, updateStatus, refresh } = usePrescriptions();
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleStatus = async (id, status) => {
    setActionError('');
    setBusy(true);
    try {
      await updateStatus(id, status);
    } catch (err) {
      setActionError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const porValidar = recetas.filter((r) => r.status === 'INGRESADA');
  const enPreparacion = recetas.filter((r) => r.status === 'EN_PREPARACION');

  if (loading) {
    return <div className={styles.state}>Cargando cola de trabajo...</div>;
  }

  return (
    <div className={styles.dashboard}>
      <h1 className={styles.title}>Cola de trabajo</h1>
      <button onClick={refresh} disabled={busy}>Actualizar conexión</button>
      {(error || actionError) && <p role="alert">{actionError || error}</p>}
      {usingMock && (
        <p className={styles.mockNotice}>
          Modo de ejemplo: el servicio no está disponible. Los cambios se guardan solo en este navegador.
        </p>
      )}
      <div className={styles.columns}>
        <section className={styles.column}>
          <h2 className={styles.columnTitle}>Recetas por validar ({porValidar.length})</h2>
          {porValidar.length === 0 && <p className={styles.empty}>No hay recetas pendientes.</p>}
          {porValidar.map((receta) => (
            <div key={receta.id} className={styles.card}>
              <div className={styles.cardHeader}>
                <span className={styles.patientName}>{receta.pacienteNombre}</span>
                <StatusBadge status={receta.status} />
              </div>
              <button
                className={styles.actionButton}
                disabled={busy}
                onClick={() => handleStatus(receta.id, 'VALIDADA')}
              >
                Validar
              </button>
            </div>
          ))}
        </section>

        <section className={styles.column}>
          <h2 className={styles.columnTitle}>En preparación ({enPreparacion.length})</h2>
          {enPreparacion.length === 0 && (
            <p className={styles.empty}>No hay recetas en preparación.</p>
          )}
          {enPreparacion.map((receta) => (
            <div key={receta.id} className={styles.card}>
              <div className={styles.cardHeader}>
                <span className={styles.patientName}>{receta.pacienteNombre}</span>
                <StatusBadge status={receta.status} />
              </div>
              <button
                className={styles.actionButton}
                disabled={busy}
                onClick={() => handleStatus(receta.id, 'LISTA_RETIRO')}
              >
                Marcar lista para retiro
              </button>
            </div>
          ))}
        </section>
      </div>
    </div>
  );
}
