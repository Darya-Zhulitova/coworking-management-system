import { NextResponse } from 'next/server';
import { BackendRequestError } from '@/lib/api/backend';
import { getUserSession, type UserSession } from '@/lib/auth/session';
import type { BookingCartItem } from '@/lib/types';

export type ApiRouteResult<T> =
  | { ok: true; value: T }
  | { ok: false; response: NextResponse };

export type CartPayload = {
  coworkingId?: number;
  items?: BookingCartItem[];
};

export async function requireApiSession(): Promise<ApiRouteResult<UserSession>> {
  const session = await getUserSession();
  if (!session) return { ok: false, response: unauthorized() };
  return { ok: true, value: session };
}

export async function readJsonBody<T>(request: Request): Promise<ApiRouteResult<T>> {
  try {
    return { ok: true, value: (await request.json()) as T };
  } catch {
    return { ok: false, response: badRequest('Invalid request body.') };
  }
}

export function parsePositiveInteger(value: string | number | null | undefined): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function parseCoworkingId(value: string | number | null | undefined): ApiRouteResult<number> {
  const coworkingId = parsePositiveInteger(value);
  if (coworkingId == null) return { ok: false, response: badRequest('Invalid coworking id.') };
  return { ok: true, value: coworkingId };
}

export function parseRequestId(value: string | number | null | undefined): ApiRouteResult<number> {
  const requestId = parsePositiveInteger(value);
  if (requestId == null) return { ok: false, response: badRequest('Invalid request id.') };
  return { ok: true, value: requestId };
}

export function normalizeCartPayload(payload: CartPayload): ApiRouteResult<{
  coworkingId: number;
  items: BookingCartItem[]
}> {
  const coworkingId = parsePositiveInteger(payload.coworkingId);
  if (coworkingId == null) return { ok: false, response: badRequest('Invalid coworking id.') };

  if (!Array.isArray(payload.items) || payload.items.length === 0) {
    return { ok: false, response: badRequest('Cart must contain at least one item.') };
  }

  const items = payload.items.map((item) => ({
    placeId: parsePositiveInteger(item.placeId),
    date: item.date,
  }));

  if (items.some((item) => item.placeId == null || !item.date)) {
    return { ok: false, response: badRequest('Cart items are invalid.') };
  }

  return {
    ok: true,
    value: {
      coworkingId,
      items: items.map((item) => ({ placeId: item.placeId!, date: item.date })),
    },
  };
}

export function okJson<T>(payload: T): NextResponse {
  return NextResponse.json(payload);
}

export function createdJson<T>(payload: T): NextResponse {
  return NextResponse.json(payload, { status: 201 });
}

export function badRequest(message: string): NextResponse {
  return NextResponse.json({ message }, { status: 400 });
}

export function unauthorized(): NextResponse {
  return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
}

export function handleApiError(error: unknown, fallbackMessage: string): NextResponse {
  if (error instanceof BackendRequestError) {
    return NextResponse.json({ message: error.message }, { status: error.status || 500 });
  }
  return NextResponse.json({ message: fallbackMessage }, { status: 500 });
}
