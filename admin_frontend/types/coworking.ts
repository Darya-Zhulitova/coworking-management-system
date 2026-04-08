export interface Coworking {
  id: number;
  name: string;
  active: boolean;
  archived: boolean;
  configurationVersion: number;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateCoworkingRequest {
  name: string;
}

export interface UpdateCoworkingRequest {
  name: string;
  active?: boolean;
}

export interface CoworkingDashboard {
  coworking: Coworking;
  grantedActions: string[];
  subjectLabel: string;
}
