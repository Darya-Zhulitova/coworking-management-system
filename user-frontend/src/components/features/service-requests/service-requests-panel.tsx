"use client";

import {useEffect, useState} from "react";
import {createServiceRequest, getServiceRequests} from "@/lib/api";
import type {CreateServiceRequestPayload, ServiceRequest} from "@/lib/types";

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
    } catch {
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
    } catch {
      setError("Не удалось создать service request.");
    }
  }

  useEffect(() => {
    void loadRequests();
  }, []);

  return (
    <section className="card shadow-sm border-0">
      <div className="card-body p-4">
        <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
          <div>
            <h3 className="h5 mb-1">Request list</h3>
            <p className="text-body-secondary mb-0">Создание и просмотр сервисных заявок.</p>
          </div>
          <button className="btn btn-outline-primary" onClick={loadRequests} disabled={loading}>
            {loading ? "Загрузка..." : "Обновить список"}
          </button>
        </div>

        <div className="row g-3">
          <div className="col-md-4">
            <label className="form-label" htmlFor="membershipId">
              Membership ID
            </label>
            <input
              id="membershipId"
              className="form-control"
              value={String(form.membershipId)}
              onChange={(event) =>
                setForm({
                  ...form,
                  membershipId: Number(event.target.value) || 0,
                })
              }
            />
          </div>

          <div className="col-md-4">
            <label className="form-label" htmlFor="placeId">
              Place ID
            </label>
            <input
              id="placeId"
              className="form-control"
              value={form.placeId ?? ""}
              onChange={(event) =>
                setForm({
                  ...form,
                  placeId: event.target.value ? Number(event.target.value) : null,
                })
              }
            />
          </div>

          <div className="col-md-4">
            <label className="form-label" htmlFor="bookingId">
              Booking ID
            </label>
            <input
              id="bookingId"
              className="form-control"
              value={form.bookingId ?? ""}
              onChange={(event) =>
                setForm({
                  ...form,
                  bookingId: event.target.value ? Number(event.target.value) : null,
                })
              }
            />
          </div>

          <div className="col-md-6">
            <label className="form-label" htmlFor="category">
              Category
            </label>
            <input
              id="category"
              className="form-control"
              value={form.category}
              onChange={(event) =>
                setForm({
                  ...form,
                  category: event.target.value,
                })
              }
            />
          </div>

          <div className="col-12">
            <label className="form-label" htmlFor="description">
              Description
            </label>
            <textarea
              id="description"
              className="form-control"
              rows={4}
              value={form.description}
              onChange={(event) =>
                setForm({
                  ...form,
                  description: event.target.value,
                })
              }
            />
          </div>
        </div>

        <div className="d-flex flex-wrap gap-2 mt-4">
          <button className="btn btn-primary" onClick={handleCreateRequest}>
            Создать заявку
          </button>
        </div>

        {message ? <div className="alert alert-success mt-3 mb-0">{message}</div> : null}
        {error ? <div className="alert alert-danger mt-3 mb-0">{error}</div> : null}

        <div className="mt-4">
          {requests.length === 0 ? (
            <p className="text-body-secondary mb-0">Пока нет данных.</p>
          ) : (
            <ul className="list-group list-group-flush">
              {requests.map((request) => (
                <li key={request.id} className="list-group-item px-0">
                  id: {request.id}, membershipId: {request.membershipId}, category: {request.category}, status:{" "}
                  {request.status}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}
