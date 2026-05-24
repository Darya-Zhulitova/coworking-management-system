export interface Coworking {
  id: number;
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string | null;
  heroText?: string | null;
  imageUrls: string[];
  uploadedImageUrls?: string[];
  uploadedImageFileIds?: string[];
  schedule: number;
  autoApproveMembership: boolean;
  floorMapEnabled: boolean;
  joinToken?: string | null;
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
  autoApproveMembership?: boolean;
  floorMapEnabled?: boolean;
}

export interface UpdateCoworkingRequest {
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string;
  heroText?: string;
  imageFileIds?: string[];
  autoApproveMembership?: boolean;
  floorMapEnabled?: boolean;
  active?: boolean;
}

export interface CoworkingJoinLink {
  coworkingId: number;
  joinToken: string | null;
  joinUrl: string | null;
}

export interface CoworkingDashboard {
  coworking: Coworking;
  grants: string[];
  subjectLabel: string;
}
