"use client";

import {useEffect, useState} from "react";
import {createMembership, getMemberships} from "@/lib/api";
import type {Membership} from "@/lib/types";

export function MembershipsPanel() {
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [coworkingId, setCoworkingId] = useState("1");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadMemberships() {
    setLoading(true);
    setError("");

    try {
      const data = await getMemberships();
      setMemberships(data);
    } catch {
      setError("Не удалось загрузить memberships.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateMembership() {
    setMessage("");
    setError("");

    try {
      const id = Number(coworkingId);

      if (!id || id <= 0) {
        setError("Введите корректный coworkingId.");
        return;
      }

      await createMembership({coworkingId: id});
      setMessage("Membership создан.");
      setCoworkingId("1");
      await loadMemberships();
    } catch {
      setError("Не удалось создать membership.");
    }
  }

  useEffect(() => {
    void loadMemberships();
  }, []);

  return (
    <section className="card shadow-sm border-0">
      <div className="card-body p-4">
        <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
          <div>
            <h3 className="h5 mb-1">Membership list</h3>
            <p className="text-body-secondary mb-0">Просмотр и создание membership для коворкинга.</p>
          </div>
          <button className="btn btn-outline-primary" onClick={loadMemberships} disabled={loading}>
            {loading ? "Загрузка..." : "Обновить список"}
          </button>
        </div>

        <div className="mb-3">
          <label className="form-label" htmlFor="coworkingId">
            Coworking ID
          </label>
          <input
            id="coworkingId"
            className="form-control"
            value={coworkingId}
            onChange={(event) => setCoworkingId(event.target.value)}
            placeholder="Например, 1"
          />
        </div>

        <div className="d-flex flex-wrap gap-2">
          <button className="btn btn-primary" onClick={handleCreateMembership}>
            Создать membership
          </button>
        </div>

        {message ? <div className="alert alert-success mt-3 mb-0">{message}</div> : null}
        {error ? <div className="alert alert-danger mt-3 mb-0">{error}</div> : null}

        <div className="mt-4">
          {memberships.length === 0 ? (
            <p className="text-body-secondary mb-0">Пока нет данных.</p>
          ) : (
            <ul className="list-group list-group-flush">
              {memberships.map((membership) => (
                <li key={membership.id} className="list-group-item px-0">
                  id: {membership.id}, coworkingId: {membership.coworkingId}, balance: {membership.balance}, status:{" "}
                  {membership.status}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}
