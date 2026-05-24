import 'server-only';

import { env } from '@/lib/config/env';
import type {
  Booking,
  BookingInitData,
  CoworkingShellContext,
  JoinCoworkingPreview,
  MembershipSummary,
  UserCoworkingDetails,
  UserProfile,
} from '@/lib/types';
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
        ? String(((data as { details: unknown[] }).details[0]) ?? 'Запрос к серверу не выполнен')
        : 'Запрос к серверу не выполнен'
      : data && typeof data === 'object' && data !== null && 'message' in data && typeof (data as {
        message?: unknown
      }).message === 'string'
        ? (data as { message: string }).message
        : 'Запрос к серверу не выполнен';
    throw new BackendRequestError(message, response.status, data);
  }
  return data as T;
}

export async function loginUser(payload: UserLoginRequest): Promise<UserAuthResponse> {
  return requestBackend<UserAuthResponse>('/api/user/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function registerUser(payload: UserRegisterRequest): Promise<UserAuthResponse> {
  return requestBackend<UserAuthResponse>('/api/user/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}


export async function getJoinCoworkingPreview(joinToken: string): Promise<JoinCoworkingPreview> {
  return requestBackend<JoinCoworkingPreview>(`/api/coworkings/join/${encodeURIComponent(joinToken)}`);
}

export async function getCoworking(membershipId: number, token: string): Promise<UserCoworkingDetails> {
  return requestBackend<UserCoworkingDetails>(`/api/memberships/${membershipId}`, undefined, token);
}

export async function getCoworkingContext(membershipId: number, token: string): Promise<CoworkingShellContext> {
  return requestBackend<CoworkingShellContext>(`/api/memberships/${membershipId}/context`, undefined, token);
}

export async function getCurrentUser(token: string): Promise<UserProfile> {
  const data = await requestBackend<{
    id: number;
    email: string;
    name: string;
    description?: string | null
  }>(`/api/user/me`, undefined, token);
  return {
    id: data.id,
    email: data.email,
    name: data.name,
    description: data.description ?? '',
  };
}

export async function getCurrentUserMemberships(token: string): Promise<MembershipSummary[]> {
  const data = await requestBackend<Array<{
    id: number;
    coworkingName: string;
    status: string;
    scheduleLabel: string;
    address: string;
    balance: number | string;
  }>>('/api/users/me/memberships', undefined, token);
  return data.map((item) => ({
    id: item.id,
    coworkingName: item.coworkingName,
    status: item.status as MembershipSummary['status'],
    scheduleLabel: item.scheduleLabel,
    address: item.address,
    balance: typeof item.balance === 'string' ? Number(item.balance) * 100 : Number(item.balance) * 100,
  }));
}

export interface BackendLedgerEntry {
  id: number;
  timestamp: string;
  type: import('@/lib/types').LedgerType;
  name: string;
  comment: string;
  amount: number;
}

export interface BackendPayRequest {
  id: number;
  amount: number;
  status: import('@/lib/types').PayRequestStatus;
  userComment: string;
  adminComment?: string | null;
  createdAt: string;
}

export interface BackendBalanceDetails {
  membershipId: number;
  membershipStatus: import('@/lib/types').MembershipStatus | Uppercase<import('@/lib/types').MembershipStatus>;
  balanceMinorUnits: number;
  ledger: BackendLedgerEntry[];
  payRequests: BackendPayRequest[];
}

export async function getBalanceDetails(membershipId: number, token: string): Promise<BackendBalanceDetails> {
  return requestBackend<BackendBalanceDetails>(`/api/memberships/${membershipId}/balance`, undefined, token);
}

export interface BackendBookingListItem {
  id: number;
  bookingNumber: string;
  placeId: number;
  placeName: string;
  placePreviewImageUrl?: string | null;
  placeFullImageUrl?: string | null;
  date: string;
  cost: number;
  active: boolean;
  status: import('@/lib/types').BookingPersistedStatus;
  requestId: string;
  pricePerDay: number;
  fullRefundHoursBefore: number;
  lateCancellationRefundPercent: number;
  cancellationPreviewMinorUnits: number;
}

function mapBooking(item: BackendBookingListItem): Booking {
  return {
    id: item.id,
    bookingNumber: item.bookingNumber,
    placeId: item.placeId,
    requestId: item.requestId,
    placeName: item.placeName,
    placePreviewImageUrl: item.placePreviewImageUrl ?? null,
    placeFullImageUrl: item.placeFullImageUrl ?? null,
    date: item.date,
    cost: item.cost,
    active: item.active,
    status: item.status,
    pricePerDay: item.pricePerDay,
    fullRefundHoursBefore: item.fullRefundHoursBefore,
    lateCancellationRefundPercent: item.lateCancellationRefundPercent,
    cancellationPreview: item.cancellationPreviewMinorUnits,
  };
}

export async function getBookingInit(membershipId: number, token: string, date?: string): Promise<BookingInitData> {
  const query = date ? `?date=${encodeURIComponent(date)}` : '';
  return requestBackend<BookingInitData>(`/api/memberships/${membershipId}/booking/init${query}`, undefined, token);
}

export async function getBookings(membershipId: number, token: string): Promise<Booking[]> {
  const data = await requestBackend<BackendBookingListItem[]>(`/api/memberships/${membershipId}/bookings`, undefined, token);
  return data.map(mapBooking);
}

export interface BackendServiceRequest {
  id: number;
  membershipId: number;
  typeId: number;
  name: string;
  typeName: string;
  cost: number;
  status: import('@/lib/types').ServiceRequestStatus | Uppercase<import('@/lib/types').ServiceRequestStatus>;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
}

export interface BackendServiceRequestAttachment {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  url: string;
}

export interface BackendServiceRequestMessage {
  id: number;
  requestId: number;
  authorType: import('@/lib/types').MessageAuthorType;
  authorName: string;
  text?: string | null;
  timestamp: string;
  readAt?: string | null;
  attachments: BackendServiceRequestAttachment[];
}

export interface BackendServiceRequestTypeOption {
  id: number;
  name: string;
  cost: number;
}

export async function getServiceRequests(membershipId: number, token: string): Promise<BackendServiceRequest[]> {
  return requestBackend<BackendServiceRequest[]>(`/api/memberships/${membershipId}/service-requests`, undefined, token);
}

export async function getServiceRequestTypes(membershipId: number, token: string): Promise<BackendServiceRequestTypeOption[]> {
  return requestBackend<BackendServiceRequestTypeOption[]>(`/api/memberships/${membershipId}/service-requests/types`, undefined, token);
}

export async function getServiceRequest(membershipId: number, requestId: number, token: string): Promise<BackendServiceRequest> {
  return requestBackend<BackendServiceRequest>(`/api/memberships/${membershipId}/service-requests/${requestId}`, undefined, token);
}

export async function getServiceRequestMessages(membershipId: number, requestId: number, token: string): Promise<BackendServiceRequestMessage[]> {
  return requestBackend<BackendServiceRequestMessage[]>(`/api/memberships/${membershipId}/service-requests/${requestId}/messages`, undefined, token);
}
