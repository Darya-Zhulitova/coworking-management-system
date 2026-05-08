export interface Coworking {
  id: number;
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string | null;
  heroText?: string | null;
  imageUrls: string[];
  schedule: number;
  autoApproveMembership: boolean;
  active: boolean;
  archived: boolean;
  configurationVersion: number;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateCoworkingRequest {
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string;
  heroText?: string;
  imageUrls: string[];
  autoApproveMembership?: boolean;
}

export interface UpdateCoworkingRequest {
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string;
  heroText?: string;
  imageUrls: string[];
  autoApproveMembership?: boolean;
  active?: boolean;
}

export interface CoworkingDashboard {
  coworking: Coworking;
  grants: string[];
  subjectLabel: string;
}
