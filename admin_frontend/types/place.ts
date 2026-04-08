export interface PlaceTypeSummary {
  id: number;
  code: string;
  name: string;
  active: boolean;
}

export interface PlaceTypeDto {
  id: number;
  coworkingId: number;
  code: string;
  name: string;
  description?: string | null;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface PlaceDto {
  id: number;
  coworkingId: number;
  name: string;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
  placeType: PlaceTypeSummary;
}

export interface CreatePlaceTypeRequest {
  code: string;
  name: string;
  description?: string;
}

export interface UpdatePlaceTypeRequest {
  code: string;
  name: string;
  description?: string;
  active?: boolean;
}

export interface CreatePlaceRequest {
  name: string;
  placeTypeId: number;
}

export interface UpdatePlaceRequest {
  name: string;
  placeTypeId?: number;
  active?: boolean;
}

export interface PlaceDeactivationPreview {
  placeId: number;
  placeName: string;
  activeBeforeChange: boolean;
  simulatedAffectedFutureBookings: number;
  plannedUserDomainCommands: string[];
  mode: string;
}

export interface CoworkingConfigSnapshot {
  coworkingId: number;
  configVersion: number;
  generatedAt: string;
  placeTypes: Array<{
    id: number;
    code: string;
    name: string;
    active: boolean;
  }>;
  places: Array<{
    id: number;
    name: string;
    active: boolean;
    placeTypeId: number;
  }>;
}
