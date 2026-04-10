import 'server-only';

import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import type { AdminLoginResponse } from '@/types/auth';

const ONE_DAY_IN_SECONDS = 60 * 60 * 24;

export const AUTH_COOKIE_NAMES = {
  token: 'admin_access_token',
  adminId: 'admin_id',
} as const;

export interface AdminSession {
  token: string;
  adminId: number;
}

function parseNumericId(value: string | undefined): number | null {
  if (!value) return null;
  const parsedValue = Number(value);
  return Number.isInteger(parsedValue) ? parsedValue : null;
}

export async function getAdminSession(): Promise<AdminSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(AUTH_COOKIE_NAMES.token)?.value;
  if (!token) return null;

  const adminId = parseNumericId(cookieStore.get(AUTH_COOKIE_NAMES.adminId)?.value);
  if (adminId == null) return null;

  return { token, adminId };
}

export async function requireAdminSession(): Promise<AdminSession> {
  const session = await getAdminSession();
  if (!session) redirect('/login');
  return session;
}

export async function setAdminSession(loginResponse: AdminLoginResponse): Promise<void> {
  const cookieStore = await cookies();
  const cookieOptions = {
    httpOnly: true,
    sameSite: 'lax' as const,
    secure: process.env.NODE_ENV === 'production',
    path: '/',
    maxAge: ONE_DAY_IN_SECONDS,
  };
  cookieStore.set(AUTH_COOKIE_NAMES.token, loginResponse.token, cookieOptions);
  cookieStore.set(AUTH_COOKIE_NAMES.adminId, String(loginResponse.adminId), cookieOptions);
}

export async function clearAdminSession(): Promise<void> {
  const cookieStore = await cookies();
  for (const cookieName of Object.values(AUTH_COOKIE_NAMES)) {
    cookieStore.delete(cookieName);
  }
}
