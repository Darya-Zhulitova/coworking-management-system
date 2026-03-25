import type {
  CreateServiceRequestPayload,
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
