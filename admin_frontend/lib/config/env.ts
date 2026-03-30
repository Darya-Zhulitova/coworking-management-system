const DEFAULT_API_BASE_URL = 'http://localhost:8081/api';

function normalizeBaseUrl(value: string): string {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

export const env = {
  apiBaseUrl: normalizeBaseUrl(
    process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL,
  ),
} as const;
