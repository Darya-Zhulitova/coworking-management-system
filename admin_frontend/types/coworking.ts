export interface Coworking {
  id: number;
  name: string;
  schedule: number;
  active: boolean;
  archived: boolean;
  configurationVersion: number;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateCoworkingRequest {
  name: string;
  schedule?: number;
}

export interface UpdateCoworkingRequest {
  name: string;
  schedule?: number;
  active?: boolean;
}

export interface CoworkingDashboard {
  coworking: Coworking;
  grants: string[];
  subjectLabel: string;
}
