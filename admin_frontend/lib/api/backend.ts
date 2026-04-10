import 'server-only';

import { env } from '@/lib/config/env';
import type { AdminLoginRequest, AdminLoginResponse } from '@/types/auth';
import type { AppContextDto } from '@/types/context';
import type { Coworking, CoworkingDashboard, CreateCoworkingRequest, UpdateCoworkingRequest } from '@/types/coworking';
import type {
  CoworkingConfigSnapshot,
  CreatePlaceRequest,
  CreatePlaceTypeRequest,
  PlaceDeactivationPreview,
  PlaceDto,
  PlaceTypeDto,
  UpdatePlaceRequest,
  UpdatePlaceTypeRequest,
} from '@/types/place';
import type { AssignCoworkingRoleRequest, CoworkingRoleDefinition, CoworkingAccessItem, UpdateCoworkingRoleRequest } from '@/types/staff';

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

export async function getAppContext(token: string, coworkingId?: number): Promise<AppContextDto> {
  const query = coworkingId == null ? '' : `?coworkingId=${coworkingId}`;
  return requestBackend<AppContextDto>(`/context${query}`, undefined, token);
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

export async function getCoworkingAccessList(token: string, coworkingId: number): Promise<CoworkingAccessItem[]> {
  return requestBackend<CoworkingAccessItem[]>(`/coworkings/${coworkingId}/staff`, undefined, token);
}

export async function getCoworkingRoles(token: string, coworkingId: number): Promise<CoworkingRoleDefinition[]> {
  return requestBackend<CoworkingRoleDefinition[]>(`/coworkings/${coworkingId}/staff/roles`, undefined, token);
}

export async function assignCoworkingRole(token: string, coworkingId: number, payload: AssignCoworkingRoleRequest): Promise<CoworkingAccessItem> {
  return requestBackend<CoworkingAccessItem>(`/coworkings/${coworkingId}/staff`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateCoworkingRole(token: string, coworkingId: number, accessId: number, payload: UpdateCoworkingRoleRequest): Promise<CoworkingAccessItem> {
  return requestBackend<CoworkingAccessItem>(`/coworkings/${coworkingId}/staff/${accessId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function deactivateCoworkingAccess(token: string, coworkingId: number, accessId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/staff/${accessId}`, { method: 'DELETE' }, token);
}

export async function getPlaceTypes(token: string, coworkingId: number): Promise<PlaceTypeDto[]> {
  return requestBackend<PlaceTypeDto[]>(`/coworkings/${coworkingId}/place-types`, undefined, token);
}

export async function createPlaceType(token: string, coworkingId: number, payload: CreatePlaceTypeRequest): Promise<PlaceTypeDto> {
  return requestBackend<PlaceTypeDto>(`/coworkings/${coworkingId}/place-types`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updatePlaceType(token: string, coworkingId: number, placeTypeId: number, payload: UpdatePlaceTypeRequest): Promise<PlaceTypeDto> {
  return requestBackend<PlaceTypeDto>(`/coworkings/${coworkingId}/place-types/${placeTypeId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function archivePlaceType(token: string, coworkingId: number, placeTypeId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/place-types/${placeTypeId}`, { method: 'DELETE' }, token);
}

export async function getPlaces(token: string, coworkingId: number, placeTypeId?: number): Promise<PlaceDto[]> {
  const query = placeTypeId == null ? '' : `?placeTypeId=${placeTypeId}`;
  return requestBackend<PlaceDto[]>(`/coworkings/${coworkingId}/places${query}`, undefined, token);
}

export async function createPlace(token: string, coworkingId: number, payload: CreatePlaceRequest): Promise<PlaceDto> {
  return requestBackend<PlaceDto>(`/coworkings/${coworkingId}/places`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updatePlace(token: string, coworkingId: number, placeId: number, payload: UpdatePlaceRequest): Promise<PlaceDto> {
  return requestBackend<PlaceDto>(`/coworkings/${coworkingId}/places/${placeId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function deactivatePlace(token: string, coworkingId: number, placeId: number): Promise<PlaceDeactivationPreview> {
  return requestBackend<PlaceDeactivationPreview>(`/coworkings/${coworkingId}/places/${placeId}/deactivate`, {
    method: 'POST',
  }, token);
}

export async function activatePlace(token: string, coworkingId: number, placeId: number): Promise<PlaceDto> {
  return requestBackend<PlaceDto>(`/coworkings/${coworkingId}/places/${placeId}/activate`, {
    method: 'POST',
  }, token);
}

export async function archivePlace(token: string, coworkingId: number, placeId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/places/${placeId}`, { method: 'DELETE' }, token);
}

export async function getCoworkingConfigSnapshot(token: string, coworkingId: number): Promise<CoworkingConfigSnapshot> {
  return requestBackend<CoworkingConfigSnapshot>(`/internal/config/coworkings/${coworkingId}/snapshot`, undefined, token);
}
