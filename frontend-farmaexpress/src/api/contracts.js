export const PRESCRIPTION_STATUSES = [
  'INGRESADA', 'VALIDADA', 'EN_PREPARACION', 'LISTA_RETIRO', 'DISPENSADA', 'RECHAZADA',
];

export const NEXT_STATUS = {
  INGRESADA: 'VALIDADA',
  VALIDADA: 'EN_PREPARACION',
  EN_PREPARACION: 'LISTA_RETIRO',
  LISTA_RETIRO: 'DISPENSADA',
};

export function normalizePrescription(prescription) {
  return {
    ...prescription,
    // The API has an identifier, not a patient name. Keep legacy mock labels readable.
    pacienteNombre: prescription.patientId || prescription.pacienteNombre || 'Sin identificar',
    fechaCreacion: prescription.createdAt?.slice(0, 10) || prescription.fechaCreacion || '',
  };
}

export function prescriptionPayload({ patientId, pharmacyId, imageUrl }) {
  const payload = {
    patientId: patientId?.trim() || '',
    pharmacyId: pharmacyId?.trim() || '',
    imageUrl: imageUrl?.trim() || null,
  };
  if (!payload.patientId || !payload.pharmacyId) {
    throw new Error('Indica el identificador del paciente y de la farmacia.');
  }
  if (payload.patientId.length > 100 || payload.pharmacyId.length > 100) {
    throw new Error('Los identificadores admiten hasta 100 caracteres.');
  }
  if (payload.imageUrl && (payload.imageUrl.length > 500 || !isHttpUrl(payload.imageUrl))) {
    throw new Error('La URL de la receta debe usar http o https y tener hasta 500 caracteres.');
  }
  return payload;
}

export function isHttpUrl(value) {
  try {
    return ['http:', 'https:'].includes(new URL(value).protocol);
  } catch {
    return false;
  }
}

export function medicamentoPayload({ sku, nombre, precio, stock }) {
  const payload = { sku: sku?.trim() || '', nombre: nombre?.trim() || '', precio: Number(precio), stock: Number(stock) };
  if (!payload.sku || !payload.nombre || payload.sku.length > 100 || payload.nombre.length > 200) {
    throw new Error('Indica un SKU (hasta 100 caracteres) y un nombre (hasta 200 caracteres).');
  }
  if (precio === '' || precio == null || !Number.isFinite(payload.precio) || payload.precio < 0
      || payload.precio >= 1e10 || Math.abs(payload.precio * 100 - Math.round(payload.precio * 100)) > 0.0001) {
    throw new Error('El precio debe ser no negativo y tener hasta dos decimales.');
  }
  if (stock === '' || stock == null || !Number.isInteger(payload.stock) || payload.stock < 0 || payload.stock > 2147483647) {
    throw new Error('El stock debe ser un entero no negativo válido.');
  }
  return payload;
}

export function canTransition(current, next) {
  return NEXT_STATUS[current] === next
    || (['INGRESADA', 'VALIDADA'].includes(current) && next === 'RECHAZADA');
}
