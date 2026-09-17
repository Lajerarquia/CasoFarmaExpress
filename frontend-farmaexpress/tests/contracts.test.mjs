import test from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizePrescription, prescriptionPayload, medicamentoPayload,
  canTransition, PRESCRIPTION_STATUSES, isHttpUrl,
} from '../src/api/contracts.js';
import { apiErrorMessage, isServiceUnavailable } from '../src/api/apiErrors.js';
import { readMock, writeMock } from '../src/utils/mockStorage.js';

test('maps backend identifiers and dates without losing the original fields', () => {
  const receta = normalizePrescription({
    id: 1, patientId: 'patient-1', pharmacyId: 'pharmacy-1',
    status: 'EN_PREPARACION', createdAt: '2026-09-16T12:30:00', imageUrl: null,
  });
  assert.equal(receta.pacienteNombre, 'patient-1');
  assert.equal(receta.fechaCreacion, '2026-09-16');
  assert.equal(receta.createdAt, '2026-09-16T12:30:00');
  assert.equal(receta.status, 'EN_PREPARACION');
});

test('keeps legacy mocks readable', () => {
  const mock = { id: 1, pacienteNombre: 'María Pérez', fechaCreacion: '2026-09-01', status: 'INGRESADA' };
  assert.deepEqual(normalizePrescription(mock), mock);
});

test('sends only the three prescription contract fields, never a local filename', () => {
  assert.deepEqual(prescriptionPayload({
    patientId: ' p1 ', pharmacyId: ' f1 ', imageUrl: ' https://example.test/rx.png ',
    archivoNombre: 'local.pdf', pacienteNombre: 'Nombre', direccion: 'Calle',
  }), { patientId: 'p1', pharmacyId: 'f1', imageUrl: 'https://example.test/rx.png' });
  assert.equal(prescriptionPayload({ patientId: 'p1', pharmacyId: 'f1' }).imageUrl, null);
});

test('rejects invalid prescription input and unsafe document links', () => {
  for (const input of [
    {}, { patientId: ' ', pharmacyId: 'f1' },
    { patientId: 'p'.repeat(101), pharmacyId: 'f1' },
    { patientId: 'p1', pharmacyId: 'f1', imageUrl: 'blob:local-file' },
    { patientId: 'p1', pharmacyId: 'f1', imageUrl: 'javascript:alert(1)' },
  ]) assert.throws(() => prescriptionPayload(input));
  assert.equal(isHttpUrl(undefined), false);
  assert.equal(isHttpUrl('https://example.test/rx.pdf'), true);
});

test('all status transitions match the existing backend workflow', () => {
  const allowed = new Set(['INGRESADA:VALIDADA', 'INGRESADA:RECHAZADA', 'VALIDADA:EN_PREPARACION',
    'VALIDADA:RECHAZADA', 'EN_PREPARACION:LISTA_RETIRO', 'LISTA_RETIRO:DISPENSADA']);
  for (const from of PRESCRIPTION_STATUSES) {
    for (const to of PRESCRIPTION_STATUSES) {
      assert.equal(canTransition(from, to), allowed.has(`${from}:${to}`));
    }
  }
});

test('catalog PUT sends a complete numeric payload without id or UI fields', () => {
  assert.deepEqual(medicamentoPayload({ id: 3, sku: ' MED-1 ', nombre: ' Medicina ', precio: '12.50', stock: '0' }),
    { sku: 'MED-1', nombre: 'Medicina', precio: 12.5, stock: 0 });
});

test('catalog rejects empty, negative, fractional stock and oversized amounts', () => {
  const valid = { sku: 'MED', nombre: 'Medicina', precio: 1, stock: 1 };
  for (const change of [
    { sku: '' }, { nombre: ' ' }, { precio: '' }, { precio: null }, { precio: -1 },
    { precio: 1.999 }, { precio: 1e10 }, { stock: '' }, { stock: -1 }, { stock: 1.2 },
  ]) assert.throws(() => medicamentoPayload({ ...valid, ...change }));
});

test('only outages activate mocks; rejected operations remain errors', () => {
  for (const status of [400, 401, 403, 404, 409, 422]) {
    assert.equal(isServiceUnavailable({ response: { status } }), false);
  }
  assert.equal(isServiceUnavailable({ code: 'ERR_NETWORK' }), true);
  assert.equal(isServiceUnavailable({ response: { status: 502 } }), true);
  assert.equal(isServiceUnavailable({ code: 'ECONNABORTED' }), true);
  assert.equal(apiErrorMessage({ response: { data: { detail: 'SKU duplicado' } } }), 'SKU duplicado');
});

test('mock fallback tolerates corrupt or unavailable local storage', () => {
  const fallback = [{ id: 1 }];
  const original = Object.getOwnPropertyDescriptor(globalThis, 'localStorage');
  try {
    Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
      getItem: () => 'not-json',
      setItem: () => { throw new Error('storage disabled'); },
    } });
    assert.deepEqual(readMock('mock', fallback), fallback);
    assert.doesNotThrow(() => writeMock('mock', fallback));
  } finally {
    if (original) Object.defineProperty(globalThis, 'localStorage', original);
    else delete globalThis.localStorage;
  }
});
