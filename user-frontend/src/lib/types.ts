export type Membership = {
  id: number;
  coworkingId: number;
  balance: string;
  status: string;
  createdAt: string;
};

export type CreateMembershipPayload = {
  coworkingId: number;
};

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


export type FinanceRequestType = "DEPOSIT" | "WITHDRAW";

export type FinanceRequestStatus = "PENDING" | "COMPLETED" | "REJECTED";

export type FinanceRequest = {
  id: number;
  membershipId: number;
  type: FinanceRequestType;
  amount: string;
  status: FinanceRequestStatus;
  comment: string;
  createdAt: string;
  processedAt: string | null;
};

export type AccountTransaction = {
  id: number;
  membershipId: number;
  type:
    | "BOOKING_DEBIT"
    | "BOOKING_REFUND"
    | "COMPENSATION"
    | "DEPOSIT_APPROVED"
    | "WITHDRAW_APPROVED";
  amount: string;
  description: string;
  createdAt: string;
};

export type BalanceOverview = {
  account: {
    membershipId: number;
    balance: string;
    currency: string;
    updatedAt: string;
  };
  pendingCount: number;
  financeRequests: FinanceRequest[];
  transactions: AccountTransaction[];
};

export type CreateFinanceRequestPayload = {
  membershipId: number;
  type: FinanceRequestType;
  amount: string;
  comment: string;
};
