import 'server-only';

import { env } from '@/lib/config/env';
import type { MembershipSummary, UserCoworkingDetails, UserProfile, } from '@/lib/types';
import type { UserAuthResponse, UserLoginRequest, UserRegisterRequest } from '@/types/auth';

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

async function requestBackend<T>(path: string, init?: RequestInit, token?: string): Promise<T> {
  const response = await fetch(`${env.backendBaseUrl}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
    cache: 'no-store',
  });
  const data = await parseJsonSafely(response);
  if (!response.ok) {
    const message = data && typeof data === 'object' && data !== null && 'details' in data
      ? Array.isArray((data as { details?: unknown }).details)
        ? String(((data as { details: unknown[] }).details[0]) ?? 'Backend request failed')
        : 'Backend request failed'
      : data && typeof data === 'object' && data !== null && 'message' in data && typeof (data as {
        message?: unknown
      }).message === 'string'
        ? (data as { message: string }).message
        : 'Backend request failed';
    throw new BackendRequestError(message, response.status, data);
  }
  return data as T;
}

export async function loginUser(payload: UserLoginRequest): Promise<UserAuthResponse> {
  return requestBackend<UserAuthResponse>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function registerUser(payload: UserRegisterRequest): Promise<UserAuthResponse> {
  return requestBackend<UserAuthResponse>('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function getCoworking(coworkingId: number, token: string): Promise<UserCoworkingDetails> {
  return requestBackend<UserCoworkingDetails>(`/api/coworkings/${coworkingId}`, undefined, token);
}

export async function getCurrentUser(token: string): Promise<UserProfile> {
  const data = await requestBackend<{
    id: number;
    email: string;
    name: string;
    description?: string | null
  }>(`/api/auth/me`, undefined, token);
  return {
    id: data.id,
    email: data.email,
    name: data.name,
    description: data.description ?? '',
    avatarLabel: data.name.trim().charAt(0).toUpperCase() || 'U',
  };
}

export async function getCurrentUserMemberships(token: string): Promise<MembershipSummary[]> {
  const data = await requestBackend<Array<{
    id: number;
    coworkingId: number;
    coworkingName: string;
    status: string;
    scheduleLabel: string;
    address: string;
    balance: number | string;
    createdAt: string
  }>>('/api/users/me/memberships', undefined, token);
  return data.map((item) => ({
    id: item.id,
    coworkingId: item.coworkingId,
    coworkingName: item.coworkingName,
    status: item.status as MembershipSummary['status'],
    scheduleLabel: item.scheduleLabel,
    address: item.address,
    balance: typeof item.balance === 'string' ? Number(item.balance) * 100 : Number(item.balance) * 100,
  }));
}