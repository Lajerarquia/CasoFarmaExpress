import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import {
  getMedicamentos,
  createMedicamento as apiCreate,
  updateMedicamento as apiUpdate,
  deleteMedicamento as apiDelete,
} from '../api/catalogApi';
import { apiErrorMessage, isServiceUnavailable } from '../api/apiErrors';
import { medicamentoPayload } from '../api/contracts';
import { readMock, writeMock } from '../utils/mockStorage';
import { CATALOG_MOCK } from '../mocks/catalogMock';

const CatalogContext = createContext(null);
const STORAGE_KEY = 'farmaexpress_catalog_mock';

export function CatalogProvider({ children }) {
  const [medicamentos, setMedicamentos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [usingMock, setUsingMock] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const data = await getMedicamentos();
      if (!Array.isArray(data)) throw new Error('La respuesta del catálogo no es válida.');
      setError('');
      setMedicamentos(data);
      setUsingMock(false);
    } catch (err) {
      if (isServiceUnavailable(err)) {
        setError('');
        setMedicamentos(readMock(STORAGE_KEY, CATALOG_MOCK));
        setUsingMock(true);
      } else {
        setMedicamentos([]);
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
    if (usingMock) writeMock(STORAGE_KEY, medicamentos);
  }, [medicamentos, usingMock]);

  const createMedicamento = async (input) => {
    if (loading) throw new Error('Espera a que termine la carga del catálogo.');
    const payload = medicamentoPayload(input);
    if (usingMock && medicamentos.some((item) => item.sku === payload.sku)) {
      throw new Error('Ya existe un medicamento con ese SKU.');
    }
    const created = usingMock ? { id: crypto.randomUUID(), ...payload } : await apiCreate(payload);
    setMedicamentos((prev) => [...prev, created]);
    return created;
  };

  const updateMedicamento = async (id, changes) => {
    if (loading) throw new Error('Espera a que termine la carga del catálogo.');
    const current = medicamentos.find((item) => item.id === id);
    if (!current) throw new Error('Medicamento no encontrado.');
    const payload = medicamentoPayload({ ...current, ...changes });
    if (usingMock && medicamentos.some((item) => item.id !== id && item.sku === payload.sku)) {
      throw new Error('Ya existe un medicamento con ese SKU.');
    }
    const updated = usingMock ? { ...current, ...payload } : await apiUpdate(id, payload);
    setMedicamentos((prev) => prev.map((item) => item.id === id ? updated : item));
    return updated;
  };

  const deleteMedicamento = async (id) => {
    if (loading) throw new Error('Espera a que termine la carga del catálogo.');
    if (!usingMock) await apiDelete(id);
    setMedicamentos((prev) => prev.filter((item) => item.id !== id));
  };

  const purchase = async (items) => {
    // Existing checkout updates stock through catalog; no order/payment endpoint exists yet.
    for (const item of items) {
      const current = medicamentos.find((medicamento) => medicamento.id === item.id);
      if (!current || !Number.isInteger(item.cantidad) || item.cantidad <= 0 || current.stock < item.cantidad) {
        throw new Error('Revisa el carrito: hay medicamentos no disponibles o sin stock suficiente.');
      }
    }
    for (const item of items) {
      const current = medicamentos.find((medicamento) => medicamento.id === item.id);
      await updateMedicamento(item.id, { stock: current.stock - item.cantidad });
    }
  };

  return (
    <CatalogContext.Provider
      value={{ medicamentos, loading, usingMock, error, createMedicamento, updateMedicamento,
        deleteMedicamento, purchase, refresh }}
    >
      {children}
    </CatalogContext.Provider>
  );
}

export function useCatalog() {
  const ctx = useContext(CatalogContext);
  if (!ctx) throw new Error('useCatalog debe usarse dentro de CatalogProvider');
  return ctx;
}
