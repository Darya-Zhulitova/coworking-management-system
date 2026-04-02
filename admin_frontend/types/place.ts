export type PlaceType = 'DESK' | 'ROOM' | 'OFFICE' | 'MEETING_ROOM' | 'OTHER';

export interface Place {
  id: number;
  coworkingId: number;
  name: string;
  type: PlaceType;
  archived?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePlaceRequest {
  coworkingId: number;
  name: string;
  type: PlaceType;
}

export interface UpdatePlaceRequest {
  name: string;
  type: PlaceType;
  archived?: boolean;
}
