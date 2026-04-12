export type MembershipStatus = 'active' | 'pending' | 'blocked';
export type BookingStatusTab = 'active' | 'history';
export type LedgerType =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'BOOKING_CHARGE'
  | 'CANCELLATION_REFUND'
  | 'DAY_CLOSURE_COMPENSATION'
  | 'MEMBERSHIP_BLOCK_COMPENSATION'
  | 'SERVICE_REQUEST_CHARGE';
export type PayRequestStatus = 'Pending' | 'Approved' | 'Rejected';
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

export type CoworkingSummary = {
  id: number;
  name: string;
  scheduleLabel: string;
  address: string;
  heroTitle: string;
  heroText: string;
  autoApproveMembership: boolean;
};

export type Place = {
  id: number;
  coworkingId: number;
  name: string;
  floor: string;
  floorId: number;
  typeName: string;
  tariffId: number;
  pricePerDay: number;
  amenities: string[];
  available: boolean;
};

export type Booking = {
  id: number;
  coworkingId: number;
  requestId: string;
  placeName: string;
  date: string;
  cost: number;
  active: boolean;
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
  name: string;
  typeName: string;
  cost: number;
  status: ServiceRequestStatus;
  createdAt: string;
  updatedAt: string;
};

export type RequestMessage = {
  id: number;
  requestId: number;
  authorType: MessageAuthorType;
  authorName: string;
  text: string;
  timestamp: string;
  readAt?: string;
};

export type ServiceRequestTypeOption = {
  id: number;
  coworkingId: number;
  name: string;
  cost: number;
};

export type BookingCartItem = {
  placeId: number;
  date: string;
};

export type CartCalculatedItem = {
  placeId: number;
  placeName: string;
  date: string;
  floor: string;
  typeName: string;
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
}

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
