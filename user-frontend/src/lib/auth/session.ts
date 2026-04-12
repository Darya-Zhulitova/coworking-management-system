import 'server-only';

import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import type { UserAuthResponse } from '@/types/auth';

const ONE_DAY_IN_SECONDS = 60 * 60 * 24;

export const AUTH_COOKIE_NAMES = {
  token: 'user_access_token',
  userId: 'user_id',
} as const;

export interface UserSession {
  token: string;
  userId: number;
}

function parseNumericId(value: string | undefined): number | null {
  if (!value) return null;
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function getUserSession(): Promise<UserSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(AUTH_COOKIE_NAMES.token)?.value;
  if (!token) return null;

  const userId = parseNumericId(cookieStore.get(AUTH_COOKIE_NAMES.userId)?.value);
  if (userId == null) return null;
  return { token, userId };
}

export async function requireUserSession(): Promise<UserSession> {
  const session = await getUserSession();
  if (!session) redirect('/login');
  return session!;
}

export async function setUserSession(loginResponse: UserAuthResponse): Promise<void> {
  const cookieStore = await cookies();
  const cookieOptions = {
    httpOnly: true,
    sameSite: 'lax' as const,
    secure: process.env.NODE_ENV === 'production',
    path: '/',
    maxAge: ONE_DAY_IN_SECONDS,
  };
  cookieStore.set(AUTH_COOKIE_NAMES.token, loginResponse.token, cookieOptions);
  cookieStore.set(AUTH_COOKIE_NAMES.userId, String(loginResponse.userId), cookieOptions);
}

export async function clearUserSession(): Promise<void> {
  const cookieStore = await cookies();
  for (const cookieName of Object.values(AUTH_COOKIE_NAMES)) {
    cookieStore.delete(cookieName);
  }
}
