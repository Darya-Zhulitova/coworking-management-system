export interface CoworkingAccessItem {
  accessId: number;
  adminId: number;
  email: string;
  name: string;
  roleId: number;
  roleName: string;
  active: boolean;
  grants: string[];
}

export interface CoworkingRoleDefinition {
  roleId: number;
  name: string;
  grants: string[];
}

export interface AssignCoworkingRoleRequest {
  email: string;
  roleId: number;
}

export interface UpdateCoworkingRoleRequest {
  roleId: number;
  active: boolean;
}
