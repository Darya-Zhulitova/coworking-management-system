import 'server-only';

import { env } from '@/lib/config/env';
import type {
  Booking,
  BookingCartItem,
  BookingInitData,
  CartCalculation,
  CheckoutResult,
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

export interface BackendLedgerEntry {
  id: number;
  coworkingId: number;
  timestamp: string;
  type: import('@/lib/types').LedgerType;
  name: string;
  comment: string;
  amount: number;
}

export interface BackendPayRequest {
  id: number;
  coworkingId: number;
  amount: number;
  status: import('@/lib/types').PayRequestStatus;
  userComment: string;
  adminComment?: string | null;
  createdAt: string;
}

export interface BackendBalanceDetails {
  membershipId: number;
  coworkingId: number;
  membershipStatus: import('@/lib/types').MembershipStatus | Uppercase<import('@/lib/types').MembershipStatus>;
  balanceMinorUnits: number;
  ledger: BackendLedgerEntry[];
  payRequests: BackendPayRequest[];
}

export async function getBalanceDetails(coworkingId: number, token: string): Promise<BackendBalanceDetails> {
  return requestBackend<BackendBalanceDetails>(`/api/coworkings/${coworkingId}/balance`, undefined, token);
}

export async function createPayRequest(payload: {
  coworkingId: number;
  amount: number;
  userComment: string
}, token: string): Promise<BackendPayRequest> {
  return requestBackend<BackendPayRequest>('/api/pay-requests', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export interface BackendBookingListItem {
  id: number;
  coworkingId: number;
  placeId: number;
  placeName: string;
  date: string;
  cost: number;
  active: boolean;
  status: import('@/lib/types').BookingPersistedStatus;
  requestId: string;
  tariffId: number;
  pricePerDay: number;
  appliedDiscountPercent: number;
  fullRefundHoursBefore: number;
  lateCancellationRefundPercent: number;
  cancellationPreviewMinorUnits: number;
}

export interface BackendCreateFromCartResponse {
  coworkingId: number;
  requestId: string;
  totalChargedMinorUnits: number;
  balanceAfterMinorUnits: number;
  bookings: BackendBookingListItem[];
}

export interface BackendCancelBookingResponse {
  bookingId: number;
  coworkingId: number;
  refundMinorUnits: number;
  balanceAfterMinorUnits: number;
  booking: BackendBookingListItem;
}

function mapBooking(item: BackendBookingListItem): Booking {
  return {
    id: item.id,
    coworkingId: item.coworkingId,
    placeId: item.placeId,
    requestId: item.requestId,
    placeName: item.placeName,
    date: item.date,
    cost: item.cost,
    active: item.active,
    status: item.status,
    tariffId: item.tariffId,
    pricePerDay: item.pricePerDay,
    appliedDiscountPercent: item.appliedDiscountPercent,
    fullRefundHoursBefore: item.fullRefundHoursBefore,
    lateCancellationRefundPercent: item.lateCancellationRefundPercent,
    cancellationPreview: item.cancellationPreviewMinorUnits,
  };
}

export async function getBookingInit(coworkingId: number, token: string, date?: string): Promise<BookingInitData> {
  const query = date ? `?date=${encodeURIComponent(date)}` : '';
  return requestBackend<BookingInitData>(`/api/coworkings/${coworkingId}/booking/init${query}`, undefined, token);
}

export async function getBookings(coworkingId: number, token: string): Promise<Booking[]> {
  const data = await requestBackend<BackendBookingListItem[]>(`/api/coworkings/${coworkingId}/bookings`, undefined, token);
  return data.map(mapBooking);
}

export async function calculateBookingCart(payload: {
  coworkingId: number;
  items: BookingCartItem[]
}, token: string): Promise<CartCalculation> {
  return requestBackend<CartCalculation>('/api/bookings/cart/calculate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function createBookingsFromCart(payload: {
  coworkingId: number;
  items: BookingCartItem[]
}, token: string): Promise<CheckoutResult> {
  const data = await requestBackend<BackendCreateFromCartResponse>('/api/bookings/create-from-cart', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
  return {
    coworkingId: data.coworkingId,
    requestId: data.requestId,
    totalChargedMinorUnits: data.totalChargedMinorUnits,
    balanceAfterMinorUnits: data.balanceAfterMinorUnits,
    bookings: data.bookings.map(mapBooking),
  };
}

export interface BackendServiceRequest {
  id: number;
  membershipId: number;
  typeId: number;
  coworkingId: number;
  name: string;
  typeName: string;
  cost: number;
  status: import('@/lib/types').ServiceRequestStatus | Uppercase<import('@/lib/types').ServiceRequestStatus>;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
}

export interface BackendServiceRequestMessage {
  id: number;
  requestId: number;
  authorType: import('@/lib/types').MessageAuthorType;
  authorName: string;
  text: string;
  timestamp: string;
  readAt?: string | null;
}

export interface BackendServiceRequestTypeOption {
  id: number;
  coworkingId: number;
  name: string;
  cost: number;
}

export async function getServiceRequests(coworkingId: number, token: string): Promise<BackendServiceRequest[]> {
  return requestBackend<BackendServiceRequest[]>(`/api/coworkings/${coworkingId}/service-requests`, undefined, token);
}

export async function getServiceRequestTypes(coworkingId: number, token: string): Promise<BackendServiceRequestTypeOption[]> {
  return requestBackend<BackendServiceRequestTypeOption[]>(`/api/coworkings/${coworkingId}/service-request-types`, undefined, token);
}

export async function getServiceRequest(coworkingId: number, requestId: number, token: string): Promise<BackendServiceRequest> {
  return requestBackend<BackendServiceRequest>(`/api/coworkings/${coworkingId}/service-requests/${requestId}`, undefined, token);
}

export async function getServiceRequestMessages(coworkingId: number, requestId: number, token: string): Promise<BackendServiceRequestMessage[]> {
  return requestBackend<BackendServiceRequestMessage[]>(`/api/coworkings/${coworkingId}/service-requests/${requestId}/messages`, undefined, token);
}

export async function createServiceRequest(payload: {
  coworkingId: number;
  typeId: number;
  name: string
}, token: string): Promise<BackendServiceRequest> {
  return requestBackend<BackendServiceRequest>('/api/service-requests', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }, token);
}

export async function createServiceRequestMessage(payload: {
  coworkingId: number;
  requestId: number;
  text: string
}, token: string): Promise<BackendServiceRequestMessage> {
  return requestBackend<BackendServiceRequestMessage>(`/api/service-requests/${payload.requestId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ coworkingId: payload.coworkingId, text: payload.text }),
  }, token);
}


export async function cancelBooking(coworkingId: number, bookingId: number, token: string): Promise<BackendCancelBookingResponse> {
  return requestBackend<BackendCancelBookingResponse>(`/api/coworkings/${coworkingId}/bookings/${bookingId}/cancel`, {
    method: 'POST',
  }, token);
}
