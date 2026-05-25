export interface TariffDto {
  id: number;
  coworkingId: number;
  name: string;
  pricePerDay: number;
  fullRefundHoursBefore: number;
  lateCancellationRefundPercent: number;
  cancellationCompensationCoefficient: number;
  dayClosureCompensationCoefficient: number;
  membershipBlockCompensationCoefficient: number;
  version: number;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface FloorDto {
  id: number;
  coworkingId: number;
  name: string;
  index: number;
  imageFileId?: string | null;
  imageUrl?: string | null;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface PlaceTypeDto {
  id: number;
  coworkingId: number;
  name: string;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
  tariff: TariffDto;
}

export interface PlaceDto {
  id: number;
  coworkingId: number;
  floorId: number;
  floorName: string;
  name: string;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
  locX?: number | null;
  locY?: number | null;
  imageFileId?: string | null;
  previewImageUrl?: string | null;
  fullImageUrl?: string | null;
  amenities?: string[];
  placeType: {
    id: number;
    name: string;
    active: boolean;
    tariffId: number;
    tariffName: string;
  };
}

export interface CoworkingScheduleExceptionDto {
  id: number;
  date: string;
  type: 'OPEN' | 'CLOSE';
  name: string;
  active: boolean;
}

export interface PlaceClosingDto {
  id: number;
  placeId: number;
  placeName: string;
  floorId: number;
  date: string;
  name: string;
  active: boolean;
}

export interface CoworkingUserReadModelDto {
  userId: number;
  name: string;
  registeredAt: string;
  balance: number;
  totalBookings: number;
  unfinishedBookings: number;
}

export interface PlaceOperationalDto {
  placeId: number;
  placeName: string;
  placeTypeName: string;
  totalBookings: number;
  unfinishedBookings: number;
  active: boolean;
  floorId: number;
}

export interface CreateFloorRequest {
  name: string;
  imageFileId?: string;
}

export interface UpdateFloorRequest extends CreateFloorRequest {
  active?: boolean;
}

export interface CreateTariffRequest {
  name: string;
  pricePerDay: number;
  fullRefundHoursBefore: number;
  lateCancellationRefundPercent: number;
  cancellationCompensationCoefficient?: number;
  dayClosureCompensationCoefficient?: number;
  membershipBlockCompensationCoefficient?: number;
}

export interface UpdateTariffRequest extends CreateTariffRequest {
  active?: boolean;
}

export interface CreatePlaceTypeRequest {
  name: string;
  tariffId: number;
}

export interface UpdatePlaceTypeRequest {
  name: string;
  active?: boolean;
}

export interface CreatePlaceRequest {
  name: string;
  floorId: number;
  placeTypeId: number;
  locX?: number | null;
  locY?: number | null;
  imageFileId?: string | null;
  imageUrl?: string | null;
  amenities?: string[];
}

export interface UpdatePlaceRequest {
  name: string;
  locX?: number | null;
  locY?: number | null;
  imageFileId?: string | null;
  imageUrl?: string | null;
  amenities?: string[];
  active?: boolean;
}

export interface CreateCoworkingScheduleExceptionRequest {
  date: string;
  type: 'OPEN' | 'CLOSE';
  name: string;
}

export interface CreatePlaceClosingRequest {
  placeId: number;
  date: string;
  name: string;
}

export interface AffectedBookingDto {
  bookingId: number;
  bookingNumber: string;
  membershipId: number;
  userId: number;
  userName: string;
  placeId: number;
  placeName: string;
  date: string;
  bookingAmount: number;
  compensationAmount: number;
}

export interface OperationalImpactDto {
  affectedBookingsCount: number;
  affectedDates: string[];
  affectedBookings: AffectedBookingDto[];
  totalCompensationAmount: number;
  impactHash: string;
}

export interface ImpactCommitRequest {
  impactHash: string;
}

export type WithImpactHash<T extends object> = T & ImpactCommitRequest;

export type PlaceDeactivationPreview = OperationalImpactDto;

export interface ServiceRequestTypeDto {
  id: number;
  coworkingId: number;
  name: string;
  cost: number;
  version: number;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateServiceRequestTypeRequest {
  name: string;
  cost: number;
}

export interface UpdateServiceRequestTypeRequest extends CreateServiceRequestTypeRequest {
  active?: boolean;
}

export interface CoworkingScheduleDto {
  schedule: number;
  monday: boolean;
  tuesday: boolean;
  wednesday: boolean;
  thursday: boolean;
  friday: boolean;
  saturday: boolean;
  sunday: boolean;
}

export interface UserQueueSummaryDto {
  usersCount: number;
  pendingMemberships: number;
  pendingPayRequests: number;
  openServiceRequests: number;
  totalBookings: number;
  activeBookings: number;
  currentBalance: number;
  monthlyIncome: number;
  monthlyOccupancyPercent: number;
}

export interface MembershipQueueItemDto {
  membershipId: number;
  userId: number;
  userName: string;
  coworkingName: string;
  status: 'pending' | 'active' | 'blocked';
  createdAt: string;
}

export interface PayRequestQueueItemDto {
  payRequestId: number;
  membershipId: number;
  userId: number;
  userName: string;
  amount: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  userComment: string;
  adminComment?: string | null;
  createdAt: string;
}

export interface ServiceRequestQueueItemDto {
  serviceRequestId: number;
  membershipId: number;
  userId: number;
  userName: string;
  typeName: string;
  name: string;
  cost: number;
  status: 'new' | 'in_progress' | 'resolved' | 'rejected';
  createdAt: string;
}

export interface ServiceRequestDetailDto {
  serviceRequestId: number;
  membershipId: number;
  userId: number;
  userName: string;
  userEmail: string;
  typeName: string;
  name: string;
  cost: number;
  balanceMinorUnits: number;
  status: 'new' | 'in_progress' | 'resolved' | 'rejected';
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
}

export interface ServiceRequestAttachmentDto {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  url: string;
}

export interface ServiceRequestMessageDto {
  id: number;
  authorType: 'USER' | 'ADMIN' | 'SYSTEM';
  authorName: string;
  text?: string | null;
  createdAt: string;
  attachments: ServiceRequestAttachmentDto[];
}

export interface ServiceRequestWorkspaceDto {
  request: ServiceRequestDetailDto;
  messages: ServiceRequestMessageDto[];
  availableActions: Array<'REPLY' | 'IN_PROGRESS' | 'RESOLVE' | 'REJECT'>;
}

export interface OperationsDashboardDto {
  queues: {
    pendingMemberships: number;
    pendingPayRequests: number;
    openServiceRequests: number;
  };
  memberships: {
    total: number;
    active: number;
    pending: number;
    blocked: number;
  };
  finance: {
    totalBalance: number;
    monthlyIncome: number;
  };
  occupancy: {
    monthlyPercent: number;
  };
}

export interface AnalyticsMetricPointDto {
  label: string;
  value: number;
}

export interface UserAnalyticsDto {
  usersCount: number;
  activeMemberships: number;
  pendingMemberships: number;
  blockedMemberships: number;
  totalBookings: number;
  activeBookings: number;
  unfinishedServiceRequests: number;
  openPayRequests: number;
  totalBalance: number;
  monthlyIncome: number;
  monthlyOccupancyPercent: number;
  monthlyIncomeHistory: AnalyticsMetricPointDto[];
  occupancyHistory: AnalyticsMetricPointDto[];
}

export interface PlaceBookingAdminDto {
  bookingId: number;
  bookingNumber: string;
  membershipId: number;
  userName: string;
  date: string;
  cost: number;
  active: boolean;
  status: string;
}

export interface PlaceBookingListResponseDto {
  coworkingId: number;
  placeId: number;
  source: 'user-service';
  message?: string | null;
  bookings: PlaceBookingAdminDto[];
}

export interface MembershipListItemDto {
  membershipId: number;
  userId: number;
  userName: string;
  userEmail?: string | null;
  status: 'ACTIVE' | 'PENDING' | 'BLOCKED' | string;
  createdAt: string;
  approvedAt?: string | null;
  blockedAt?: string | null;
  balanceMinorUnits: number;
  activeBookingsCount: number;
}

export interface MembershipBookingDto {
  bookingId: number;
  bookingNumber: string;
  placeId: number;
  placeName: string;
  date: string;
  cost: number;
  status: string;
}

export interface MembershipProfileDto {
  membershipId: number;
  userId: number;
  userName: string;
  userEmail?: string | null;
  userDescription?: string | null;
  status: 'ACTIVE' | 'PENDING' | 'BLOCKED' | string;
  createdAt: string;
  approvedAt?: string | null;
  blockedAt?: string | null;
  balanceMinorUnits: number;
  activeBookings: MembershipBookingDto[];
}
