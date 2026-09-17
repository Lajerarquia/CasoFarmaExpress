import axios from 'axios';
import { msalInstance } from '../auth/AuthProvider';
import { loginRequest, isAzureConfigured } from '../auth/msalConfig';

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
});

httpClient.interceptors.request.use(async (config) => {
  if (!isAzureConfigured) return config;

  const accounts = msalInstance.getAllAccounts();

  if (accounts.length === 0) {
    return config;
  }

  try {
    const response = await msalInstance.acquireTokenSilent({
      ...loginRequest,
      account: accounts[0],
    });
    config.headers.Authorization = `Bearer ${response.accessToken}`;
  } catch {
    // El token expiró y no se pudo renovar en silencio: manda de nuevo al login
    await msalInstance.acquireTokenRedirect(loginRequest);
  }

  return config;
});

export default httpClient;
