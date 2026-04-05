export interface AccessibleCoworking {
  id: number;
  name: string;
  assignmentType: 'OWNER' | 'ROLE_ASSIGNED';
  role: 'MANAGER' | 'STAFF_SUPPORT' | null;
  owner: boolean;
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
