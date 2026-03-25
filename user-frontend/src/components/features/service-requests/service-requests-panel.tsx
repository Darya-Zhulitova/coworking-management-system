"use client";

import { useEffect, useState } from "react";
import { createServiceRequest, getServiceRequests } from "@/lib/api";
import type { CreateServiceRequestPayload, ServiceRequest } from "@/lib/types";

const initialForm: CreateServiceRequestPayload = {
  membershipId: 1,
  placeId: null,
  bookingId: null,
  category: "",
  description: "",
};

export function ServiceRequestsPanel() {
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [form, setForm] = useState<CreateServiceRequestPayload>(initialForm);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadRequests() {
    setLoading(true);
    setError("");

    try {
      const data = await getServiceRequests();
      setRequests(data);
    } catch (e) {
      setError("Не удалось загрузить service requests.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateRequest() {
    setMessage("");
    setError("");

    if (!form.membershipId || form.membershipId <= 0) {
      setError("membershipId должен быть положительным числом.");
      return;
    }

    if (!form.category.trim() || !form.description.trim()) {
      setError("Заполните category и description.");
      return;
    }

    try {
      await createServiceRequest(form);
      setMessage("Service request создан.");
      setForm(initialForm);
      await loadRequests();
    } catch (e) {
      setError("Не удалось создать service request.");
    }
  }

  useEffect(() => {
    loadRequests();
  }, []);

  return (
    <div className="panel">
      <h3>Request list</h3>

      <div className="row">
        <button className="button" onClick={loadRequests} disabled={loading}>
          {loading ? "Загрузка..." : "Обновить список"}
        </button>
      </div>

      <div className="row">
        <label className="label" htmlFor="membershipId">
          Membership ID
        </label>
        <input
          id="membershipId"
          className="input"
          value={String(form.membershipId)}
          onChange={(event) =>
            setForm({
              ...form,
              membershipId: Number(event.target.value) || 0,
            })
          }
        />
      </div>

      <div className="row">
        <label className="label" htmlFor="placeId">
          Place ID
        </label>
        <input
          id="placeId"
          className="input"
          value={form.placeId ?? ""}
          onChange={(event) =>
            setForm({
              ...form,
              placeId: event.target.value ? Number(event.target.value) : null,
            })
          }
        />
      </div>

      <div className="row">
        <label className="label" htmlFor="bookingId">
          Booking ID
        </label>
        <input
          id="bookingId"
          className="input"
          value={form.bookingId ?? ""}
          onChange={(event) =>
            setForm({
              ...form,
              bookingId: event.target.value ? Number(event.target.value) : null,
            })
          }
        />
      </div>

      <div className="row">
        <label className="label" htmlFor="category">
          Category
        </label>
        <input
          id="category"
          className="input"
          value={form.category}
          onChange={(event) =>
            setForm({
              ...form,
              category: event.target.value,
            })
          }
        />
      </div>

      <div className="row">
        <label className="label" htmlFor="description">
          Description
        </label>
        <textarea
          id="description"
          className="textarea"
          value={form.description}
          onChange={(event) =>
            setForm({
              ...form,
              description: event.target.value,
            })
          }
        />
      </div>

      <div className="button-row">
        <button className="button" onClick={handleCreateRequest}>
          Создать заявку
        </button>
      </div>

      {message ? <p className="message">{message}</p> : null}
      {error ? <p className="error">{error}</p> : null}

      <div className="row" style={{ marginTop: 16 }}>
        {requests.length === 0 ? (
          <p className="muted">Пока нет данных.</p>
        ) : (
          <ul className="list">
            {requests.map((request) => (
              <li key={request.id}>
                id: {request.id}, membershipId: {request.membershipId}, category:{" "}
                {request.category}, status: {request.status}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
