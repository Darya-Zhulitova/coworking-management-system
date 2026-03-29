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
    } catch (e) {
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
    } catch (e) {
      setError("Не удалось создать membership.");
    }
  }

  useEffect(() => {
    loadMemberships();
  }, []);

  return (
    <div className="panel">
      <h3>Membership list</h3>

      <div className="row">
        <button className="button" onClick={loadMemberships} disabled={loading}>
          {loading ? "Загрузка..." : "Обновить список"}
        </button>
      </div>

      <div className="row">
        <label className="label" htmlFor="coworkingId">
          Coworking ID
        </label>
        <input
          id="coworkingId"
          className="input"
          value={coworkingId}
          onChange={(event) => setCoworkingId(event.target.value)}
          placeholder="Например, 1"
        />
      </div>

      <div className="button-row">
        <button className="button" onClick={handleCreateMembership}>
          Создать membership
        </button>
      </div>

      {message ? <p className="message">{message}</p> : null}
      {error ? <p className="error">{error}</p> : null}

      <div className="row" style={{marginTop: 16}}>
        {memberships.length === 0 ? (
          <p className="muted">Пока нет данных.</p>
        ) : (
          <ul className="list">
            {memberships.map((membership) => (
              <li key={membership.id}>
                id: {membership.id}, coworkingId: {membership.coworkingId}, balance:{" "}
                {membership.balance}, status: {membership.status}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
