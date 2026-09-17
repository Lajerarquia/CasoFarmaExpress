export function isServiceUnavailable(error) {
  return ['ERR_NETWORK', 'ECONNABORTED', 'ETIMEDOUT'].includes(error?.code)
    || error?.response?.status >= 500;
}

export function apiErrorMessage(error) {
  const body = error?.response?.data;
  if (typeof body?.detail === 'string') return body.detail;
  if (typeof body?.message === 'string') return body.message;
  if (isServiceUnavailable(error)) return 'No se pudo contactar con el servicio. Intenta nuevamente.';
  return error?.message || 'No se pudo completar la operación.';
}
