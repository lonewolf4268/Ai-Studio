import { Capacitor } from '@capacitor/core';

const STORAGE_KEY = 'ai_studio_server_url';
export const DEFAULT_EMULATOR_URL = 'http://10.0.2.2:3000';

export function isNativeApp(): boolean {
  return Capacitor.isNativePlatform();
}

export function getApiBaseUrl(): string {
  const customUrl = localStorage.getItem(STORAGE_KEY);
  if (customUrl) {
    return customUrl.trim().replace(/\/+$/, '');
  }

  const envUrl = import.meta.env.VITE_API_URL;
  if (envUrl) {
    return envUrl.trim().replace(/\/+$/, '');
  }

  // If running inside native Android, default to Android emulator loopback host URL
  if (isNativeApp()) {
    return DEFAULT_EMULATOR_URL;
  }

  // Running on Web browser: relative path works out of the box
  return '';
}

export function setApiBaseUrl(url: string): void {
  const trimmed = url.trim().replace(/\/+$/, '');
  if (!trimmed) {
    localStorage.removeItem(STORAGE_KEY);
  } else {
    localStorage.setItem(STORAGE_KEY, trimmed);
  }
}

export function getEffectiveApiUrl(endpoint: string): string {
  const base = getApiBaseUrl();
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  if (!base) {
    return cleanEndpoint;
  }
  return `${base}${cleanEndpoint}`;
}
