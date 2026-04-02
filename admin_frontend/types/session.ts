import type { AccessibleCoworking, AdminPrincipalType } from '@/types/auth';

export interface AdminSessionDto {
  token: string;
  adminUserId: number | null;
  superAdminId: number | null;
  principalType: AdminPrincipalType;
  coworkings: AccessibleCoworking[];
  grantedGlobalActions: string[];
}
