export class ClientRequestError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
    this.name = 'ClientRequestError';
  }
}

export async function requestJson<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.headers ?? {}),
    },
    cache: 'no-store',
  });

  const data = await response.json().catch(() => null) as { message?: string } | T | null;
  if (!response.ok) {
    const message = data && typeof data === 'object' && 'message' in data && typeof data.message === 'string'
      ? data.message
      : 'Request failed.';
    throw new ClientRequestError(message, response.status);
  }

  return data as T;
}
