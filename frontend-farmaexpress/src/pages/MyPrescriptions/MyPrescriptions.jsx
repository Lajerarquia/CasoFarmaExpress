import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usePrescriptions } from '../../context/PrescriptionsContext';
import { apiErrorMessage } from '../../api/apiErrors';
import { isHttpUrl } from '../../api/contracts';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import styles from './MyPrescriptions.module.css';

export default function MyPrescriptions() {
  const { user } = useAuth();
  const { recetas, loading, usingMock, error, createReceta, refresh } = usePrescriptions();
  const [patientId, setPatientId] = useState(user?.email || '');
  const [pharmacyId, setPharmacyId] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [confirmado, setConfirmado] = useState('');

  const misRecetas = recetas.filter((receta) =>
    receta.patientId
      ? receta.patientId === patientId.trim()
      : usingMock && receta.pacienteNombre === user?.nombre
  );

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitError('');
    setConfirmado('');
    setEnviando(true);
    try {
      await createReceta({ patientId, pharmacyId, imageUrl });
      setImageUrl('');
      setConfirmado(usingMock
        ? 'Receta de ejemplo guardada en este navegador; no se envió al servidor.'
        : 'Receta registrada. Puedes seguir su estado en el historial.');
    } catch (err) {
      setSubmitError(apiErrorMessage(err));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <h1 className={styles.title}>Solicita tu receta</h1>
        <p className={styles.subtitle}>
          Registra tu receta indicando el paciente, la farmacia y, si la tienes, la URL del documento.
        </p>
      </div>
      {usingMock && (
        <p className={styles.mockNotice} role="status">
          Modo de ejemplo: el servicio no está disponible. Los cambios se guardan solo en este navegador.
        </p>
      )}
      {error && <p role="alert" className={styles.errorText}>{error}</p>}
      <button type="button" onClick={refresh} disabled={loading || enviando}>
        {loading ? 'Conectando...' : 'Actualizar conexión'}
      </button>

      <div className={styles.grid}>
        <form className={styles.form} onSubmit={handleSubmit}>
          <h2 className={styles.formTitle}>Nueva receta</h2>
          <label className={styles.label}>
            Identificador del paciente
            <input className={styles.input} value={patientId} maxLength={100} required
              disabled={enviando}
              onChange={(event) => { setPatientId(event.target.value); setConfirmado(''); }} />
          </label>
          <p className={styles.hint}>
            Usa siempre el mismo identificador para consultar tus recetas. Se propone tu correo de sesión.
          </p>
          <label className={styles.label}>
            Identificador de la farmacia
            <input className={styles.input} value={pharmacyId} maxLength={100} required
              disabled={enviando} onChange={(event) => setPharmacyId(event.target.value)} />
          </label>
          <label className={styles.label}>
            URL de la receta (opcional)
            <input className={styles.input} type="url" value={imageUrl} maxLength={500}
              placeholder="https://..." disabled={enviando}
              onChange={(event) => setImageUrl(event.target.value)} />
          </label>
          <p className={styles.hint}>
            Por ahora puedes indicar un enlace existente. La carga de archivos y el despacho aún no están disponibles.
          </p>
          {submitError && <p role="alert" className={styles.errorText}>{submitError}</p>}
          <button className={styles.submitButton} type="submit" disabled={enviando || loading || Boolean(error)}>
            {enviando ? 'Enviando...' : usingMock ? 'Guardar receta de ejemplo' : 'Registrar receta'}
          </button>
          {confirmado && <p role="status" className={styles.success}>{confirmado}</p>}
        </form>

        <div className={styles.history}>
          <h2 className={styles.formTitle}>Recetas del paciente ({misRecetas.length})</h2>
          {loading && <p className={styles.state}>Cargando...</p>}
          {!loading && misRecetas.length === 0 && <p className={styles.empty}>No hay recetas para este identificador.</p>}
          {misRecetas.map((receta) => (
            <div key={receta.id} className={styles.historyCard}>
              <div className={styles.historyHeader}>
                <span className={styles.historyDate}>{receta.fechaCreacion}</span>
                <StatusBadge status={receta.status} />
              </div>
              <p className={styles.historyDetail}>Paciente: {receta.pacienteNombre}</p>
              <p className={styles.historyDetail}>Farmacia: {receta.pharmacyId || receta.farmacia || 'Sin indicar'}</p>
              {isHttpUrl(receta.imageUrl) && (
                <a href={receta.imageUrl} target="_blank" rel="noopener noreferrer">Ver receta</a>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
