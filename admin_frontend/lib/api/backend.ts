import 'server-only';

import { env } from '@/lib/config/env';
import type { AdminLoginRequest, AdminLoginResponse } from '@/types/auth';

export class BackendRequestError extends Error {
  constructor(message: string, public readonly status: number, public readonly details?: unknown) {
    super(message);
    this.name = 'BackendRequestError';
  }
}

async function parseJsonSafely(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) return null;
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export async function loginAdmin(payload: AdminLoginRequest): Promise<AdminLoginResponse> {
  const response = await fetch(`${env.apiBaseUrl}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(payload),
    cache: 'no-store',
  });

  const data = await parseJsonSafely(response);

  if (!response.ok) {
    const message = data && typeof data === 'object' && 'message' in data && typeof data.message === 'string' ? data.message : 'Login failed';
    throw new BackendRequestError(message, response.status, data);
  }

  if (!data || typeof data !== 'object') {
    throw new BackendRequestError('Unexpected login response', response.status);
  }

  return data as AdminLoginResponse;
}
