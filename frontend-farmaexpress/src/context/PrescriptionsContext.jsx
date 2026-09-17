import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import {
  getPrescriptions,
  getPrescriptionById,
  createPrescription as apiCreate,
  updatePrescriptionStatus as apiUpdateStatus,
} from '../api/prescriptionsApi';
import { apiErrorMessage, isServiceUnavailable } from '../api/apiErrors';
import { canTransition, normalizePrescription, prescriptionPayload } from '../api/contracts';
import { readMock, writeMock } from '../utils/mockStorage';
import { PRESCRIPTIONS_MOCK } from '../mocks/prescriptionsMock';

const PrescriptionsContext = createContext(null);
const STORAGE_KEY = 'farmaexpress_prescriptions_mock';

export function PrescriptionsProvider({ children }) {
  const [recetas, setRecetas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [usingMock, setUsingMock] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const data = await getPrescriptions();
      if (!Array.isArray(data)) throw new Error('La respuesta de recetas no es válida.');
      setError('');
      setRecetas(data.map(normalizePrescription));
      setUsingMock(false);
    } catch (err) {
      if (isServiceUnavailable(err)) {
        setError('');
        setRecetas(readMock(STORAGE_KEY, PRESCRIPTIONS_MOCK).map(normalizePrescription));
        setUsingMock(true);
      } else {
        setRecetas([]);
        setUsingMock(false);
        setError(apiErrorMessage(err));
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const refresh = () => {
    setLoading(true);
    setError('');
    return load();
  };

  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (usingMock) writeMock(STORAGE_KEY, recetas);
  }, [recetas, usingMock]);

  const getReceta = async (id) => {
    if (loading) throw new Error('Espera a que termine la carga de recetas.');
    if (usingMock) {
      const receta = recetas.find((item) => item.id === id);
      if (!receta) throw new Error('Receta no encontrada.');
      return receta;
    }
    const receta = normalizePrescription(await getPrescriptionById(id));
    setRecetas((prev) => prev.map((item) => item.id === id ? receta : item));
    return receta;
  };

  const createReceta = async (input) => {
    if (loading) throw new Error('Espera a que termine la carga de recetas.');
    const payload = prescriptionPayload(input);
    const nueva = normalizePrescription(usingMock
      ? { ...payload, id: crypto.randomUUID(), status: 'INGRESADA', createdAt: new Date().toISOString() }
      : await apiCreate(payload));
    setRecetas((prev) => [nueva, ...prev]);
    return nueva;
  };

  const updateStatus = async (id, status) => {
    if (loading) throw new Error('Espera a que termine la carga de recetas.');
    const current = recetas.find((item) => item.id === id);
    if (!current) throw new Error('Receta no encontrada.');
    if (!canTransition(current.status, status)) throw new Error('Cambio de estado no permitido.');
    const updated = usingMock
      ? { ...current, status, updatedAt: new Date().toISOString() }
      : normalizePrescription(await apiUpdateStatus(id, status));
    setRecetas((prev) => prev.map((item) => item.id === id ? updated : item));
    return updated;
  };

  return (
    <PrescriptionsContext.Provider
      value={{ recetas, loading, usingMock, error, getReceta, createReceta, updateStatus, refresh }}
    >
      {children}
    </PrescriptionsContext.Provider>
  );
}

export function usePrescriptions() {
  const ctx = useContext(PrescriptionsContext);
  if (!ctx) throw new Error('usePrescriptions debe usarse dentro de PrescriptionsProvider');
  return ctx;
}
