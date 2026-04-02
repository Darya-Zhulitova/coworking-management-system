import 'server-only';

import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import type { AccessibleCoworking, AdminLoginResponse, AdminPrincipalType } from '@/types/auth';

const ONE_DAY_IN_SECONDS = 60 * 60 * 24;

export const AUTH_COOKIE_NAMES = {
  token: 'admin_access_token',
  adminUserId: 'admin_user_id',
  superAdminId: 'super_admin_id',
  principalType: 'admin_principal_type',
  coworkings: 'admin_coworkings',
  grantedGlobalActions: 'admin_global_actions',
} as const;

export interface AdminSession {
  token: string;
  adminUserId: number | null;
  superAdminId: number | null;
  principalType: AdminPrincipalType;
  coworkings: AccessibleCoworking[];
  grantedGlobalActions: string[];
}

function parseNumericId(value: string | undefined): number | null {
  if (!value) return null;
  const parsedValue = Number(value);
  return Number.isInteger(parsedValue) ? parsedValue : null;
}

function parsePrincipalType(value: string | undefined): AdminPrincipalType | null {
  return value === 'TENANT_ADMIN' || value === 'SUPERADMIN' ? value : null;
}

function parseCoworkings(value: string | undefined): AccessibleCoworking[] {
  if (!value) return [];
  try {
    const parsedValue = JSON.parse(value) as unknown;
    if (!Array.isArray(parsedValue)) return [];
    return parsedValue.filter((item): item is AccessibleCoworking => {
      if (!item || typeof item !== 'object') return false;
      const maybeCoworking = item as Partial<AccessibleCoworking>;
      return typeof maybeCoworking.id === 'number' && typeof maybeCoworking.name === 'string' && typeof maybeCoworking.role === 'string';
    });
  } catch {
    return [];
  }
}

function parseStringArray(value: string | undefined): string[] {
  if (!value) return [];
  try {
    const parsedValue = JSON.parse(value) as unknown;
    return Array.isArray(parsedValue) ? parsedValue.filter((item): item is string => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

export async function getAdminSession(): Promise<AdminSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(AUTH_COOKIE_NAMES.token)?.value;
  const principalType = parsePrincipalType(cookieStore.get(AUTH_COOKIE_NAMES.principalType)?.value);
  if (!token || !principalType) return null;

  const adminUserId = parseNumericId(cookieStore.get(AUTH_COOKIE_NAMES.adminUserId)?.value);
  const superAdminId = parseNumericId(cookieStore.get(AUTH_COOKIE_NAMES.superAdminId)?.value);

  return {
    token,
    adminUserId,
    superAdminId,
    principalType,
    coworkings: parseCoworkings(cookieStore.get(AUTH_COOKIE_NAMES.coworkings)?.value),
    grantedGlobalActions: parseStringArray(cookieStore.get(AUTH_COOKIE_NAMES.grantedGlobalActions)?.value),
  };
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
  cookieStore.set(AUTH_COOKIE_NAMES.principalType, loginResponse.principalType, cookieOptions);
  cookieStore.set(AUTH_COOKIE_NAMES.adminUserId, loginResponse.adminUserId == null ? '' : String(loginResponse.adminUserId), cookieOptions);
  cookieStore.set(AUTH_COOKIE_NAMES.superAdminId, loginResponse.superAdminId == null ? '' : String(loginResponse.superAdminId), cookieOptions);
  cookieStore.set(AUTH_COOKIE_NAMES.coworkings, JSON.stringify(loginResponse.coworkings), cookieOptions);
  cookieStore.set(AUTH_COOKIE_NAMES.grantedGlobalActions, JSON.stringify(loginResponse.grantedGlobalActions ?? []), cookieOptions);
}

export async function clearAdminSession(): Promise<void> {
  const cookieStore = await cookies();
  for (const cookieName of Object.values(AUTH_COOKIE_NAMES)) {
    cookieStore.delete(cookieName);
  }
}

export function hasGlobalAction(session: AdminSession, action: string): boolean {
  return session.grantedGlobalActions.includes(action);
}
