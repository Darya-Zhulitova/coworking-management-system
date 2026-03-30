export interface AccessibleCoworking {
  id: number;
  name: string;
  role: string;
}

export type AdminPrincipalType = 'TENANT_ADMIN' | 'SUPERADMIN';

export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface AdminLoginResponse {
  token: string;
  adminUserId: number | null;
  superAdminId: number | null;
  principalType: AdminPrincipalType;
  grantedGlobalActions: string[];
  coworkings: AccessibleCoworking[];
}
