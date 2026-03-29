import type {
  CreateMembershipPayload,
  CreateServiceRequestPayload,
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
