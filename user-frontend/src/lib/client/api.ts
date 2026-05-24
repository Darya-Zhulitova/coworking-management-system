export class ClientRequestError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
    this.name = 'ClientRequestError';
  }
}

function toBackendProxyPath(path: string): string {
  if (!path.startsWith('/api/')) {
    return path;
  }
  return `/api/backend/${path.slice('/api/'.length)}`;
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(toBackendProxyPath(path), {
    ...init,
    headers: { Accept: 'application/json', ...(init?.headers ?? {}) },
    cache: 'no-store'
  });
  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json') ? await response.json().catch(() => null) : null;
  if (!response.ok) {
    const message = payload && typeof payload === 'object' && payload !== null && 'message' in payload && typeof (payload as {
      message?: unknown
    }).message === 'string'
      ? (payload as { message: string }).message
      : payload && typeof payload === 'object' && payload !== null && 'details' in payload && Array.isArray((payload as {
        details?: unknown
      }).details)
        ? String(((payload as { details: unknown[] }).details[0]) ?? 'Запрос не выполнен')
        : 'Запрос не выполнен';
    throw new ClientRequestError(message, response.status);
  }
  return payload as T;
}
