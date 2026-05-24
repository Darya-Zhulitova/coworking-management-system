import 'server-only';

import { env } from '@/lib/config/env';
import type { AdminLoginRequest, AdminLoginResponse } from '@/types/auth';
import type { AppContextDto } from '@/types/context';
import type {
  Coworking,
  CoworkingDashboard,
  CoworkingJoinLink,
  CreateCoworkingRequest,
  UpdateCoworkingRequest
} from '@/types/coworking';
import type {
  CoworkingScheduleDto,
  CoworkingScheduleExceptionDto,
  CoworkingUserReadModelDto,
  CreateCoworkingScheduleExceptionRequest,
  CreateFloorRequest,
  CreatePlaceClosingRequest,
  CreatePlaceRequest,
  CreatePlaceTypeRequest,
  CreateServiceRequestTypeRequest,
  CreateTariffRequest,
  FloorDto,
  ImpactCommitRequest,
  MembershipListItemDto,
  MembershipProfileDto,
  MembershipQueueItemDto,
  OperationalImpactDto,
  OperationsDashboardDto,
  PayRequestQueueItemDto,
  PlaceBookingListResponseDto,
  PlaceClosingDto,
  PlaceDeactivationPreview,
  PlaceDto,
  PlaceOperationalDto,
  PlaceTypeDto,
  ServiceRequestDetailDto,
  ServiceRequestMessageDto,
  ServiceRequestQueueItemDto,
  ServiceRequestTypeDto,
  ServiceRequestWorkspaceDto,
  TariffDto,
  UpdateFloorRequest,
  UpdatePlaceRequest,
  UpdatePlaceTypeRequest,
  UpdateServiceRequestTypeRequest,
  UpdateTariffRequest,
  UserAnalyticsDto,
  UserQueueSummaryDto,
  WithImpactHash,
} from '@/types/place';
import type {
  AssignCoworkingRoleRequest,
  CoworkingAccessItem,
  CoworkingRoleDefinition,
  CreateRoleRequest,
  UpdateCoworkingRoleRequest,
  UpdateRoleRequest
} from '@/types/staff';

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
    const message = data && typeof data === 'object' && 'message' in data && typeof (data as {
      message?: unknown
    }).message === 'string'
      ? (data as { message: string }).message
      : 'Запрос к серверу не выполнен';
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


export async function getCoworkingJoinLink(token: string, id: number): Promise<CoworkingJoinLink> {
  return requestBackend<CoworkingJoinLink>(`/coworkings/${id}/join-link`, undefined, token);
}

export async function generateCoworkingJoinLink(token: string, id: number): Promise<CoworkingJoinLink> {
  return requestBackend<CoworkingJoinLink>(`/coworkings/${id}/join-link`, { method: 'POST' }, token);
}

export async function deleteCoworkingJoinLink(token: string, id: number): Promise<CoworkingJoinLink> {
  return requestBackend<CoworkingJoinLink>(`/coworkings/${id}/join-link`, { method: 'DELETE' }, token);
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


export async function uploadCoworkingPhoto(token: string, coworkingId: number, formData: FormData): Promise<Coworking> {
  return requestBackend<Coworking>(`/coworkings/${coworkingId}/photos`, {
    method: 'POST',
    body: formData,
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


export async function createCoworkingRole(token: string, coworkingId: number, payload: CreateRoleRequest): Promise<CoworkingRoleDefinition> {
  return requestBackend<CoworkingRoleDefinition>(`/coworkings/${coworkingId}/staff/roles`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateCoworkingRoleDefinition(token: string, coworkingId: number, roleId: number, payload: UpdateRoleRequest): Promise<CoworkingRoleDefinition> {
  return requestBackend<CoworkingRoleDefinition>(`/coworkings/${coworkingId}/staff/roles/${roleId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}


export async function archiveCoworkingRole(token: string, coworkingId: number, roleId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/staff/roles/${roleId}`, { method: 'DELETE' }, token);
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


export async function getFloors(token: string, coworkingId: number): Promise<FloorDto[]> {
  return requestBackend<FloorDto[]>(`/coworkings/${coworkingId}/floors`, undefined, token);
}

export async function createFloor(token: string, coworkingId: number, payload: CreateFloorRequest): Promise<FloorDto> {
  return requestBackend<FloorDto>(`/coworkings/${coworkingId}/floors`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateFloor(token: string, coworkingId: number, floorId: number, payload: UpdateFloorRequest): Promise<FloorDto> {
  return requestBackend<FloorDto>(`/coworkings/${coworkingId}/floors/${floorId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function archiveFloor(token: string, coworkingId: number, floorId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/floors/${floorId}`, { method: 'DELETE' }, token);
}

export async function uploadFloorPlan(token: string, coworkingId: number, floorId: number, formData: FormData): Promise<FloorDto> {
  return requestBackend<FloorDto>(`/coworkings/${coworkingId}/floors/${floorId}/plan`, {
    method: 'POST',
    body: formData,
  }, token);
}

export async function getTariffs(token: string, coworkingId: number): Promise<TariffDto[]> {
  return requestBackend<TariffDto[]>(`/coworkings/${coworkingId}/tariffs`, undefined, token);
}

export async function getTariff(token: string, coworkingId: number, tariffId: number): Promise<TariffDto> {
  return requestBackend<TariffDto>(`/coworkings/${coworkingId}/tariffs/${tariffId}`, undefined, token);
}

export async function createTariff(token: string, coworkingId: number, payload: CreateTariffRequest): Promise<TariffDto> {
  return requestBackend<TariffDto>(`/coworkings/${coworkingId}/tariffs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateTariff(token: string, coworkingId: number, tariffId: number, payload: UpdateTariffRequest): Promise<TariffDto> {
  return requestBackend<TariffDto>(`/coworkings/${coworkingId}/tariffs/${tariffId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function archiveTariff(token: string, coworkingId: number, tariffId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/tariffs/${tariffId}`, { method: 'DELETE' }, token);
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

export async function uploadPlacePhoto(token: string, coworkingId: number, placeId: number, formData: FormData): Promise<PlaceDto> {
  return requestBackend<PlaceDto>(`/coworkings/${coworkingId}/places/${placeId}/photo`, {
    method: 'POST',
    body: formData,
  }, token);
}

export async function getPlaceBookings(token: string, coworkingId: number, placeId: number): Promise<PlaceBookingListResponseDto> {
  return requestBackend<PlaceBookingListResponseDto>(`/coworkings/${coworkingId}/places/${placeId}/bookings`, undefined, token);
}

export async function previewDeactivatePlace(token: string, coworkingId: number, placeId: number): Promise<PlaceDeactivationPreview> {
  return requestBackend<PlaceDeactivationPreview>(`/coworkings/${coworkingId}/places/${placeId}/deactivation/preview`, { method: 'POST' }, token);
}

export async function commitDeactivatePlace(token: string, coworkingId: number, placeId: number, payload: ImpactCommitRequest): Promise<PlaceDeactivationPreview> {
  return requestBackend<PlaceDeactivationPreview>(`/coworkings/${coworkingId}/places/${placeId}/deactivation/commit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function deactivatePlace(token: string, coworkingId: number, placeId: number, payload: ImpactCommitRequest): Promise<PlaceDeactivationPreview> {
  return commitDeactivatePlace(token, coworkingId, placeId, payload);
}

export async function activatePlace(token: string, coworkingId: number, placeId: number): Promise<PlaceDto> {
  return requestBackend<PlaceDto>(`/coworkings/${coworkingId}/places/${placeId}/activate`, {
    method: 'POST',
  }, token);
}

export async function archivePlace(token: string, coworkingId: number, placeId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/places/${placeId}`, { method: 'DELETE' }, token);
}


export async function getCoworkingSchedule(token: string, coworkingId: number): Promise<CoworkingScheduleDto> {
  return requestBackend<CoworkingScheduleDto>(`/coworkings/${coworkingId}/schedule`, undefined, token);
}

export async function getCoworkingScheduleExceptions(token: string, coworkingId: number): Promise<CoworkingScheduleExceptionDto[]> {
  return requestBackend<CoworkingScheduleExceptionDto[]>(`/coworkings/${coworkingId}/schedule/exceptions`, undefined, token);
}

export async function createCoworkingScheduleException(token: string, coworkingId: number, payload: CreateCoworkingScheduleExceptionRequest): Promise<CoworkingScheduleExceptionDto> {
  return requestBackend<CoworkingScheduleExceptionDto>(`/coworkings/${coworkingId}/schedule/exceptions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }, token);
}


export async function archiveCoworkingScheduleException(token: string, coworkingId: number, exceptionId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/schedule/exceptions/${exceptionId}`, { method: 'DELETE' }, token);
}

export async function getPlaceClosings(token: string, coworkingId: number, floorId?: number): Promise<PlaceClosingDto[]> {
  const query = floorId == null ? '' : `?floorId=${floorId}`;
  return requestBackend<PlaceClosingDto[]>(`/coworkings/${coworkingId}/schedule/closings${query}`, undefined, token);
}

export async function archivePlaceClosing(token: string, coworkingId: number, closingId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/schedule/closings/${closingId}`, { method: 'DELETE' }, token);
}


export async function getMembershipList(token: string, coworkingId: number, search?: string): Promise<MembershipListItemDto[]> {
  const query = search?.trim() ? `?search=${encodeURIComponent(search.trim())}` : '';
  return requestBackend<MembershipListItemDto[]>(`/coworkings/${coworkingId}/memberships${query}`, undefined, token);
}

export async function getMembershipProfile(token: string, coworkingId: number, membershipId: number): Promise<MembershipProfileDto> {
  return requestBackend<MembershipProfileDto>(`/coworkings/${coworkingId}/memberships/${membershipId}`, undefined, token);
}

export async function adjustMembershipBalance(token: string, coworkingId: number, membershipId: number, payload: {
  amountMinorUnits: number;
  comment?: string | null
}): Promise<MembershipProfileDto> {
  return requestBackend<MembershipProfileDto>(`/coworkings/${coworkingId}/memberships/${membershipId}/balance-adjustments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function previewMembershipBlock(token: string, coworkingId: number, membershipId: number): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/memberships/${membershipId}/block-preview`, {
    method: 'POST',
  }, token);
}

export async function blockMembership(token: string, coworkingId: number, membershipId: number, payload: ImpactCommitRequest): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/memberships/${membershipId}/block`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function getCoworkingUsers(token: string, coworkingId: number): Promise<CoworkingUserReadModelDto[]> {
  return requestBackend<CoworkingUserReadModelDto[]>(`/coworkings/${coworkingId}/users`, undefined, token);
}

export async function getPlaceOperationalItems(token: string, coworkingId: number, floorId?: number): Promise<PlaceOperationalDto[]> {
  const query = floorId == null ? '' : `?floorId=${floorId}`;
  return requestBackend<PlaceOperationalDto[]>(`/coworkings/${coworkingId}/places/operational${query}`, undefined, token);
}


export async function getServiceRequestTypes(token: string, coworkingId: number): Promise<ServiceRequestTypeDto[]> {
  return requestBackend<ServiceRequestTypeDto[]>(`/coworkings/${coworkingId}/service-request-types`, undefined, token);
}

export async function createServiceRequestType(token: string, coworkingId: number, payload: CreateServiceRequestTypeRequest): Promise<ServiceRequestTypeDto> {
  return requestBackend<ServiceRequestTypeDto>(`/coworkings/${coworkingId}/service-request-types`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function updateServiceRequestType(token: string, coworkingId: number, serviceRequestTypeId: number, payload: UpdateServiceRequestTypeRequest): Promise<ServiceRequestTypeDto> {
  return requestBackend<ServiceRequestTypeDto>(`/coworkings/${coworkingId}/service-request-types/${serviceRequestTypeId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function archiveServiceRequestType(token: string, coworkingId: number, serviceRequestTypeId: number): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/service-request-types/${serviceRequestTypeId}`, { method: 'DELETE' }, token);
}

export async function getOperationsDashboard(token: string, coworkingId: number): Promise<OperationsDashboardDto> {
  return requestBackend<OperationsDashboardDto>(`/coworkings/${coworkingId}/operations-dashboard`, undefined, token);
}

export async function getMembershipQueue(token: string, coworkingId: number): Promise<MembershipQueueItemDto[]> {
  return requestBackend<MembershipQueueItemDto[]>(`/coworkings/${coworkingId}/membership-requests`, undefined, token);
}

export async function decideMembership(token: string, coworkingId: number, membershipId: number, decision: 'APPROVE' | 'REJECT', comment?: string): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/membership-requests/${membershipId}/decision`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision, comment: comment ?? null })
  }, token);
}

export async function getPayRequestQueue(token: string, coworkingId: number): Promise<PayRequestQueueItemDto[]> {
  return requestBackend<PayRequestQueueItemDto[]>(`/coworkings/${coworkingId}/pay-requests`, undefined, token);
}

export async function decidePayRequest(token: string, coworkingId: number, payRequestId: number, decision: 'APPROVE' | 'REJECT', comment?: string): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/pay-requests/${payRequestId}/decision`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision, comment: comment ?? null })
  }, token);
}

export async function getServiceRequestQueue(token: string, coworkingId: number): Promise<ServiceRequestQueueItemDto[]> {
  return requestBackend<ServiceRequestQueueItemDto[]>(`/coworkings/${coworkingId}/service-requests`, undefined, token);
}

export async function getServiceRequestWorkspace(token: string, coworkingId: number, serviceRequestId: number): Promise<ServiceRequestWorkspaceDto> {
  return requestBackend<ServiceRequestWorkspaceDto>(`/coworkings/${coworkingId}/service-requests/${serviceRequestId}/workspace`, undefined, token);
}

export async function createServiceRequestMessage(
  token: string,
  coworkingId: number,
  serviceRequestId: number,
  formData: FormData,
): Promise<ServiceRequestMessageDto> {
  return requestBackend<ServiceRequestMessageDto>(
    `/coworkings/${coworkingId}/service-requests/${serviceRequestId}/messages`,
    {
      method: 'POST',
      body: formData,
    },
    token,
  );
}

export async function decideServiceRequest(token: string, coworkingId: number, serviceRequestId: number, decision: 'IN_PROGRESS' | 'RESOLVE' | 'REJECT', comment?: string): Promise<void> {
  await requestBackend<void>(`/coworkings/${coworkingId}/service-requests/${serviceRequestId}/decision`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision, comment: comment ?? null })
  }, token);
}

export async function previewCoworkingScheduleUpdate(token: string, coworkingId: number, payload: Omit<CoworkingScheduleDto, 'schedule'> & Partial<Pick<CoworkingScheduleDto, 'schedule'>>): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/preview`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function commitCoworkingScheduleUpdate(token: string, coworkingId: number, payload: WithImpactHash<Omit<CoworkingScheduleDto, 'schedule'> & Partial<Pick<CoworkingScheduleDto, 'schedule'>>>): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/commit`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function previewCoworkingScheduleException(token: string, coworkingId: number, payload: CreateCoworkingScheduleExceptionRequest): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/exceptions/preview`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function commitCoworkingScheduleException(token: string, coworkingId: number, payload: WithImpactHash<CreateCoworkingScheduleExceptionRequest>): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/exceptions/commit`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function previewPlaceClosing(token: string, coworkingId: number, payload: CreatePlaceClosingRequest): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/closings/preview`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function commitPlaceClosing(token: string, coworkingId: number, payload: WithImpactHash<CreatePlaceClosingRequest>): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/closings/commit`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function previewCoworkingCloseDay(token: string, coworkingId: number, payload: {
  date: string;
  name: string
}): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/close-day/preview`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function commitCoworkingCloseDay(token: string, coworkingId: number, payload: WithImpactHash<{
  date: string;
  name: string
}>): Promise<OperationalImpactDto> {
  return requestBackend<OperationalImpactDto>(`/coworkings/${coworkingId}/schedule/close-day/commit`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  }, token);
}

export async function getUserQueueSummary(token: string, coworkingId: number): Promise<UserQueueSummaryDto> {
  const dashboard = await getOperationsDashboard(token, coworkingId);
  return {
    usersCount: dashboard.memberships.total,
    pendingMemberships: dashboard.queues.pendingMemberships,
    pendingPayRequests: dashboard.queues.pendingPayRequests,
    openServiceRequests: dashboard.queues.openServiceRequests,
    totalBookings: 0,
    activeBookings: 0,
    currentBalance: dashboard.finance.totalBalance,
    monthlyIncome: dashboard.finance.monthlyIncome,
    monthlyOccupancyPercent: dashboard.occupancy.monthlyPercent,
  };
}

export async function getUserAnalytics(token: string, coworkingId: number): Promise<UserAnalyticsDto> {
  const dashboard = await getOperationsDashboard(token, coworkingId);
  return {
    usersCount: dashboard.memberships.total,
    activeMemberships: dashboard.memberships.active,
    pendingMemberships: dashboard.memberships.pending,
    blockedMemberships: dashboard.memberships.blocked,
    totalBookings: 0,
    activeBookings: 0,
    unfinishedServiceRequests: dashboard.queues.openServiceRequests,
    openPayRequests: dashboard.queues.pendingPayRequests,
    totalBalance: dashboard.finance.totalBalance,
    monthlyIncome: dashboard.finance.monthlyIncome,
    monthlyOccupancyPercent: dashboard.occupancy.monthlyPercent,
    monthlyIncomeHistory: [],
    occupancyHistory: [],
  };
}

export async function approveMembership(token: string, coworkingId: number, membershipId: number): Promise<void> {
  await decideMembership(token, coworkingId, membershipId, 'APPROVE');
}

export async function rejectMembership(token: string, coworkingId: number, membershipId: number): Promise<void> {
  await decideMembership(token, coworkingId, membershipId, 'REJECT');
}

export async function approvePayRequest(token: string, coworkingId: number, payRequestId: number, comment?: string): Promise<void> {
  await decidePayRequest(token, coworkingId, payRequestId, 'APPROVE', comment);
}

export async function rejectPayRequest(token: string, coworkingId: number, payRequestId: number, comment?: string): Promise<void> {
  await decidePayRequest(token, coworkingId, payRequestId, 'REJECT', comment);
}

export async function getServiceRequestDetails(token: string, coworkingId: number, serviceRequestId: number): Promise<ServiceRequestDetailDto> {
  const workspace = await getServiceRequestWorkspace(token, coworkingId, serviceRequestId);
  return workspace.request;
}

export async function getServiceRequestMessages(token: string, coworkingId: number, serviceRequestId: number): Promise<ServiceRequestMessageDto[]> {
  const workspace = await getServiceRequestWorkspace(token, coworkingId, serviceRequestId);
  return workspace.messages;
}

export async function updateServiceRequestStatus(token: string, coworkingId: number, serviceRequestId: number, status: string): Promise<void> {
  const normalized = status.trim().toUpperCase();
  const decision = normalized === 'RESOLVED' ? 'RESOLVE' : normalized === 'REJECTED' ? 'REJECT' : 'IN_PROGRESS';
  await decideServiceRequest(token, coworkingId, serviceRequestId, decision);
}
