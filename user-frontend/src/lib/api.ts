import type {
  BalanceOverview,
  CreateFinanceRequestPayload,
  CreateMembershipPayload,
  CreateServiceRequestPayload,
  FinanceRequest,
  Membership,
  ServiceRequest,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers || {}),
    },
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json();
}

export function getMemberships(): Promise<Membership[]> {
  return request("/api/memberships");
}

export function createMembership(
  payload: CreateMembershipPayload
): Promise<Membership> {
  return request("/api/memberships", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getServiceRequests(): Promise<ServiceRequest[]> {
  return request("/api/service-requests");
}

export function createServiceRequest(
  payload: CreateServiceRequestPayload
): Promise<ServiceRequest> {
  return request("/api/service-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}


async function localRequest<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers || {}),
    },
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;

    try {
      const errorData = await response.json();
      if (errorData?.message) {
        message = errorData.message;
      }
    } catch {
    }

    throw new Error(message);
  }

  return response.json();
}

export function getBalanceOverview(): Promise<BalanceOverview> {
  return localRequest("/api/balance/overview");
}

export function createFinanceRequest(
  payload: CreateFinanceRequestPayload
): Promise<FinanceRequest> {
  return localRequest("/api/balance/finance-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
