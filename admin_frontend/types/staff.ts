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
  active: boolean;
}

export interface AssignCoworkingRoleRequest {
  email: string;
  roleId: number;
}

export interface UpdateCoworkingRoleRequest {
  roleId: number;
  active: boolean;
}

export interface CreateRoleRequest {
  name: string;
  grants: string[];
  active?: boolean;
}

export interface UpdateRoleRequest extends CreateRoleRequest {
  active: boolean;
}
