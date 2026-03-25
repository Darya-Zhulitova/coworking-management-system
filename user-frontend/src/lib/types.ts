export type ServiceRequest = {
  id: number;
  membershipId: number;
  placeId: number | null;
  bookingId: number | null;
  category: string;
  description: string;
  status: string;
  createdAt: string;
};

export type CreateServiceRequestPayload = {
  membershipId: number;
  placeId: number | null;
  bookingId: number | null;
  category: string;
  description: string;
};
