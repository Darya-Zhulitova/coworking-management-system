export type MembershipStatus = 'active' | 'pending' | 'blocked';
export type BookingPersistedStatus = 'ACTUAL' | 'CANCELED_ADMIN' | 'CANCELED_USER';
export type LedgerType =
  | 'BALANCE_TOP_UP'
  | 'BALANCE_WITHDRAWAL'
  | 'BOOKING_CHARGE'
  | 'BOOKING_USER_CANCELLATION_REFUND'
  | 'BOOKING_ADMIN_CANCELLATION_COMPENSATION'
  | 'MANUAL_CREDIT'
  | 'MANUAL_DEBIT'
  | 'SERVICE_REQUEST_CHARGE';
export type PayRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type ServiceRequestStatus = 'new' | 'in_progress' | 'resolved' | 'rejected';
export type MessageAuthorType = 'USER' | 'ADMIN' | 'SYSTEM';

export type UserProfile = {
  id: number;
  name: string;
  email: string;
  description: string;
};

export type MembershipSummary = {
  id: number;
  coworkingName: string;
  status: MembershipStatus;
  scheduleLabel: string;
  address: string;
  balance: number;
};

export type Booking = {
  id: number;
  bookingNumber?: string;
  placeId?: number;
  requestId: string;
  placeName: string;
  placePreviewImageUrl?: string | null;
  placeFullImageUrl?: string | null;
  date: string;
  cost: number;
  active: boolean;
  status: BookingPersistedStatus;
  pricePerDay?: number;
  fullRefundHoursBefore: number;
  lateCancellationRefundPercent: number;
  cancellationPreview?: number;
};

export type LedgerEntry = {
  id: number;
  timestamp: string;
  type: LedgerType;
  name: string;
  comment: string;
  amount: number;
};

export type PayRequest = {
  id: number;
  amount: number;
  status: PayRequestStatus;
  userComment: string;
  createdAt: string;
  adminComment?: string | null;
};

export type ServiceRequest = {
  id: number;
  membershipId?: number;
  typeId?: number;
  name: string;
  typeName: string;
  cost: number;
  status: ServiceRequestStatus;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
};

export type BookingCartItem = {
  placeId: number;
  date: string;
};

export type BookingInitFloor = {
  id: number;
  name: string;
  index?: number | null;
  imageFileId?: string | null;
  imageUrl?: string | null;
  active: boolean;
};

export type BookingInitPlace = {
  id: number;
  name: string;
  floorId?: number;
  floorName: string;
  placeTypeId?: number;
  placeTypeName: string;
  pricePerDay: number;
  amenities: string[];
  locX?: number | null;
  locY?: number | null;
  imageFileId?: string | null;
  imageUrl?: string | null;
  previewImageUrl?: string | null;
  fullImageUrl?: string | null;
  active: boolean;
  available: boolean;
};

export type PlaceAvailabilityDay = {
  date: string;
  available: boolean;
};

export type BookingInitData = {
  coworkingName: string;
  membershipId: number;
  membershipStatus: MembershipStatus;
  balanceMinorUnits: number;
  previewDate: string;
  floorMapEnabled: boolean;
  floors: BookingInitFloor[];
  places: BookingInitPlace[];
};

export type CartCalculatedItem = {
  placeId: number;
  placeName: string;
  placePreviewImageUrl?: string | null;
  placeFullImageUrl?: string | null;
  date: string;
  floor: string;
  typeName: string;
  finalPrice: number;
  available: boolean;
};

export type CartCalculationSummary = {
  totalFinalPrice: number;
  unavailableCount: number;
  validationErrors: string[];
  hasEnoughBalance: boolean;
  balanceAfterMinorUnits: number;
  canCheckout: boolean;
};

export type CartCalculation = {
  items: CartCalculatedItem[];
  summary: CartCalculationSummary;
};

export type CheckoutResult = {
  membershipId: number;
  requestId: string;
  totalChargedMinorUnits: number;
  balanceAfterMinorUnits: number;
  bookings: Booking[];
};

export type UserCoworkingDetails = {
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string | null;
  heroText?: string | null;
  imageUrls: string[];
  active: boolean;
  membershipId?: number | null;
  membershipStatus?: MembershipStatus | null;
  balanceMinorUnits?: number | null;
};

export type CoworkingShellContext = {
  user: UserProfile;
  coworking: UserCoworkingDetails;
  membership: {
    id: number | null;
    status: MembershipStatus | null;
    balanceMinorUnits: number;
  };
};

export type JoinCoworkingPreview = {
  coworkingId: number;
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string | null;
  heroText?: string | null;
  imageUrls: string[];
  autoApproveMembership: boolean;
  active: boolean;
};

export type JoinCoworkingResult = {
  membershipId: number;
  coworkingId: number;
  status: MembershipStatus;
  existingMembership: boolean;
};
