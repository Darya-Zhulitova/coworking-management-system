const fallbackBackendBaseUrl = process.env.USER_BACKEND_BASE_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export const env = {
  backendBaseUrl: fallbackBackendBaseUrl.replace(/\/$/, ''),
} as const;
