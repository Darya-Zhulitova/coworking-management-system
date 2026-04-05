export type TenantAssignmentType = 'OWNER' | 'ROLE_ASSIGNED';
export type TenantRoleCode = 'MANAGER' | 'STAFF_SUPPORT';

export interface TenantStaffMember {
  accessId: number;
  adminUserId: number;
  email: string;
  assignmentType: TenantAssignmentType;
  role: TenantRoleCode | null;
  owner: boolean;
  active: boolean;
  grantedActions: string[];
}

export interface TenantRoleDefinition {
  code: TenantRoleCode;
  label: string;
  grantedActions: string[];
}

export interface AssignTenantRoleRequest {
  email: string;
  role: TenantRoleCode;
}

export interface UpdateTenantRoleRequest {
  role: TenantRoleCode;
  active: boolean;
}
