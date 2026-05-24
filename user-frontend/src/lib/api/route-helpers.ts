import { NextResponse } from 'next/server';
import { BackendRequestError } from '@/lib/api/backend';
import { getUserSession, type UserSession } from '@/lib/auth/session';

export type ApiRouteResult<T> =
  | { ok: true; value: T }
  | { ok: false; response: NextResponse };

export async function requireApiSession(): Promise<ApiRouteResult<UserSession>> {
  const session = await getUserSession();
  if (!session) return { ok: false, response: unauthorized() };
  return { ok: true, value: session };
}

export async function readJsonBody<T>(request: Request): Promise<ApiRouteResult<T>> {
  try {
    return { ok: true, value: (await request.json()) as T };
  } catch {
    return { ok: false, response: badRequest('Некорректное тело запроса.') };
  }
}

export function okJson<T>(payload: T): NextResponse {
  return NextResponse.json(payload);
}

export function badRequest(message: string): NextResponse {
  return NextResponse.json({ message }, { status: 400 });
}

export function unauthorized(): NextResponse {
  return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
}

export function handleApiError(error: unknown, fallbackMessage: string): NextResponse {
  if (error instanceof BackendRequestError) {
    return NextResponse.json({ message: error.message }, { status: error.status || 500 });
  }
  return NextResponse.json({ message: fallbackMessage }, { status: 500 });
}
