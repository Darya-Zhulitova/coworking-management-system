import 'server-only';

import { env } from '@/lib/config/env';
import type { AdminLoginRequest, AdminLoginResponse } from '@/types/auth';
import type { Coworking, CoworkingDashboard, CreateCoworkingRequest, UpdateCoworkingRequest } from '@/types/coworking';
import type { AssignTenantRoleRequest, TenantRoleDefinition, TenantStaffMember, UpdateTenantRoleRequest } from '@/types/staff';

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
  const headers = new Headers(init?.headers ?? {});
  headers.set('Accept', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...init,
    headers,
    cache: 'no-store',
  });

  const data = await parseJsonSafely(response);
  if (!response.ok) {
    const message = data && typeof data === 'object' && 'message' in data && typeof (data as { message?: unknown }).message === 'string'
      ? (data as { message: string }).message
      : 'Backend request failed';
    throw new BackendRequestError(message, response.status, data);
  }

  return data as T;
}

export async function loginAdmin(payload: AdminLoginRequest): Promise<AdminLoginResponse> {
  return requestBackend<AdminLoginResponse>('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function getCoworkings(token: string, archived = false): Promise<Coworking[]> {
  return requestBackend<Coworking[]>(`/coworkings?archived=${archived ? 'true' : 'false'}`, undefined, token);
}

export async function getCoworking(token: string, id: number): Promise<Coworking> {
  return requestBackend<Coworking>(`/coworkings/${id}`, undefined, token);
}

export async function getCoworkingDashboard(token: string, id: number): Promise<CoworkingDashboard> {
  return requestBackend<CoworkingDashboard>(`/coworkings/${id}/dashboard`, undefined, token);
}

export async function createCoworking(token: string, payload: CreateCoworkingRequest): Promise<Coworking> {
  return requestBackend<Coworking>('/coworkings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateCoworking(token: string, id: number, payload: UpdateCoworkingRequest): Promise<Coworking> {
  return requestBackend<Coworking>(`/coworkings/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function archiveCoworking(token: string, id: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${id}`, { method: 'DELETE' }, token);
}

export async function getTenantStaff(token: string, coworkingId: number): Promise<TenantStaffMember[]> {
  return requestBackend<TenantStaffMember[]>(`/coworkings/${coworkingId}/staff`, undefined, token);
}

export async function getTenantRoles(token: string, coworkingId: number): Promise<TenantRoleDefinition[]> {
  return requestBackend<TenantRoleDefinition[]>(`/coworkings/${coworkingId}/staff/roles`, undefined, token);
}

export async function assignTenantRole(token: string, coworkingId: number, payload: AssignTenantRoleRequest): Promise<TenantStaffMember> {
  return requestBackend<TenantStaffMember>(`/coworkings/${coworkingId}/staff`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateTenantRole(token: string, coworkingId: number, accessId: number, payload: UpdateTenantRoleRequest): Promise<TenantStaffMember> {
  return requestBackend<TenantStaffMember>(`/coworkings/${coworkingId}/staff/${accessId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function deactivateTenantAccess(token: string, coworkingId: number, accessId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/staff/${accessId}`, { method: 'DELETE' }, token);
}
