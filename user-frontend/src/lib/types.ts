export type MembershipStatus = 'active' | 'pending' | 'blocked';
export type BookingPersistedStatus = 'ACTUAL' | 'CANCELED_ADMIN' | 'CANCELED_USER';
export type LedgerType =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'BOOKING_CHARGE'
  | 'CANCELLATION_REFUND'
  | 'DAY_CLOSURE_COMPENSATION'
  | 'MEMBERSHIP_BLOCK_COMPENSATION'
  | 'SERVICE_REQUEST_CHARGE';
export type PayRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type ServiceRequestStatus = 'new' | 'in_progress' | 'resolved' | 'rejected';
export type MessageAuthorType = 'USER' | 'ADMIN' | 'SYSTEM';

export type UserProfile = {
  id: number;
  name: string;
  email: string;
  description: string;
  avatarLabel: string;
};

export type MembershipSummary = {
  id: number;
  coworkingId: number;
  coworkingName: string;
  status: MembershipStatus;
  scheduleLabel: string;
  address: string;
  balance: number;
};

export type Booking = {
  id: number;
  coworkingId: number;
  placeId?: number;
  requestId: string;
  placeName: string;
  date: string;
  cost: number;
  active: boolean;
  status: BookingPersistedStatus;
  tariffId?: number;
  pricePerDay?: number;
  appliedDiscountPercent?: number;
  fullRefundHoursBefore: number;
  lateCancellationRefundPercent: number;
  cancellationPreview?: number;
};

export type LedgerEntry = {
  id: number;
  coworkingId: number;
  timestamp: string;
  type: LedgerType;
  name: string;
  comment: string;
  amount: number;
};

export type PayRequest = {
  id: number;
  coworkingId: number;
  amount: number;
  status: PayRequestStatus;
  userComment: string;
  createdAt: string;
};

export type ServiceRequest = {
  id: number;
  coworkingId: number;
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
  index: number;
};

export type BookingInitPlaceType = {
  id: number;
  name: string;
  tariffId: number;
};

export type BookingInitTariffDiscountRule = {
  id: number;
  thresholdQuantity: number;
  discountPercent: number;
};

export type BookingInitTariff = {
  id: number;
  name: string;
  pricePerDay: number;
  minBookingDays: number;
  discountRules: BookingInitTariffDiscountRule[];
};

export type BookingInitPlace = {
  id: number;
  name: string;
  floorId: number;
  floorName: string;
  placeTypeId: number;
  placeTypeName: string;
  tariffId: number;
  pricePerDay: number;
  amenities: string[];
  active: boolean;
  previewAvailable: boolean;
};

export type BookingInitData = {
  coworkingId: number;
  coworkingName: string;
  membershipId: number;
  membershipStatus: MembershipStatus;
  balanceMinorUnits: number;
  previewDate: string;
  floors: BookingInitFloor[];
  placeTypes: BookingInitPlaceType[];
  tariffs: BookingInitTariff[];
  places: BookingInitPlace[];
};

export type CartCalculatedItem = {
  placeId: number;
  placeName: string;
  date: string;
  floor: string;
  typeName: string;
  tariffId: number;
  basePrice: number;
  discountPercent: number;
  discountAmount: number;
  finalPrice: number;
  available: boolean;
};

export type CartCalculationSummary = {
  totalBasePrice: number;
  totalDiscount: number;
  totalFinalPrice: number;
  unavailableCount: number;
  discountHints: string[];
  validationErrors: string[];
  hasEnoughBalance: boolean;
  balanceAfterMinorUnits: number;
  canCheckout: boolean;
};

export type CartCalculation = {
  coworkingId: number;
  items: CartCalculatedItem[];
  summary: CartCalculationSummary;
};

export type CheckoutResult = {
  coworkingId: number;
  requestId: string;
  totalChargedMinorUnits: number;
  balanceAfterMinorUnits: number;
  bookings: Booking[];
};

export type UserCoworkingDetails = {
  id: number;
  name: string;
  description: string;
  address: string;
  workingHoursLabel: string;
  heroTitle?: string | null;
  heroText?: string | null;
  imageUrls: string[];
  autoApproveMembership: boolean;
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
