"use client";

import {useEffect, useMemo, useState} from "react";
import {createFinanceRequest, getBalanceOverview} from "@/lib/api";
import type {BalanceOverview, CreateFinanceRequestPayload, FinanceRequestType,} from "@/lib/types";

const initialForm: CreateFinanceRequestPayload = {
  membershipId: 1,
  type: "DEPOSIT",
  amount: "1500",
  comment: "",
};

const requestTypeLabels: Record<FinanceRequestType, string> = {
  DEPOSIT: "Пополнение",
  WITHDRAW: "Вывод",
};

const requestStatusLabels: Record<string, string> = {
  PENDING: "Ожидание",
  COMPLETED: "Завершено",
  REJECTED: "Отклонено",
};

const transactionTypeLabels: Record<string, string> = {
  BOOKING_DEBIT: "Списание за бронь",
  BOOKING_REFUND: "Возврат за бронь",
  COMPENSATION: "Компенсация",
  DEPOSIT_APPROVED: "Подтвержденное пополнение",
  WITHDRAW_APPROVED: "Подтвержденный вывод",
};

function formatMoney(value: string) {
  return new Intl.NumberFormat("ru-RU", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function BalancePanel() {
  const [overview, setOverview] = useState<BalanceOverview | null>(null);
  const [form, setForm] = useState<CreateFinanceRequestPayload>(initialForm);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const pendingWithdrawTotal = useMemo(() => {
    if (!overview) {
      return 0;
    }

    return overview.financeRequests
      .filter((request) => request.type === "WITHDRAW" && request.status === "PENDING")
      .reduce((sum, request) => sum + Number(request.amount), 0);
  }, [overview]);

  async function loadOverview() {
    setLoading(true);
    setError("");

    try {
      const data = await getBalanceOverview();
      setOverview(data);
    } catch (e) {
      setError("Не удалось загрузить баланс.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit() {
    setMessage("");
    setError("");

    const amount = Number(form.amount);

    if (!Number.isFinite(amount) || amount <= 0) {
      setError("Введите корректную сумму больше 0.");
      return;
    }

    if (form.membershipId <= 0) {
      setError("membershipId должен быть положительным числом.");
      return;
    }

    setSubmitting(true);

    try {
      await createFinanceRequest({
        ...form,
        amount: amount.toFixed(2),
      });

      setMessage(
        form.type === "DEPOSIT"
          ? "Запрос на пополнение создан. Баланс изменится после подтверждения менеджером."
          : "Запрос на вывод создан. Доступный баланс пересчитается после проверки."
      );
      setForm(initialForm);
      await loadOverview();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось создать финансовый запрос.");
    } finally {
      setSubmitting(false);
    }
  }

  useEffect(() => {
    loadOverview();
  }, []);

  return (
    <div className="balance-layout">
      <section className="panel">
        <div className="section-header">
          <div>
            <h3>Состояние счета</h3>
          </div>
          <button className="button" onClick={loadOverview} disabled={loading}>
            {loading ? "Загрузка..." : "Обновить"}
          </button>
        </div>

        {overview ? (
          <div className="stats-grid">
            <article className="stat-card">
              <span className="stat-label">Текущий баланс</span>
              <strong className="stat-value">{formatMoney(overview.account.balance)} ₽</strong>
            </article>
            <article className="stat-card">
              <span className="stat-label">Доступно сейчас</span>
              <strong className="stat-value">
                {formatMoney(String(Number(overview.account.balance) - pendingWithdrawTotal))} ₽
              </strong>
            </article>
            <article className="stat-card">
              <span className="stat-label">Запросов в ожидании</span>
              <strong className="stat-value">{overview.pendingCount}</strong>
            </article>
            <article className="stat-card">
              <span className="stat-label">Membership</span>
              <strong className="stat-value">#{overview.account.membershipId}</strong>
            </article>
          </div>
        ) : (
          <p className="muted">Нет данных по счету.</p>
        )}

        {message ? <p className="message">{message}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </section>

      <section className="panel">
        <h3>Создать финансовый запрос</h3>

        <div className="row">
          <label className="label" htmlFor="membershipId-balance">
            Membership ID
          </label>
          <input
            id="membershipId-balance"
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
          <label className="label" htmlFor="requestType">
            Тип запроса
          </label>
          <select
            id="requestType"
            className="select"
            value={form.type}
            onChange={(event) =>
              setForm({
                ...form,
                type: event.target.value as FinanceRequestType,
              })
            }
          >
            <option value="DEPOSIT">Пополнение</option>
            <option value="WITHDRAW">Вывод</option>
          </select>
        </div>

        <div className="row">
          <label className="label" htmlFor="amount">
            Сумма
          </label>
          <input
            id="amount"
            className="input"
            value={form.amount}
            onChange={(event) =>
              setForm({
                ...form,
                amount: event.target.value,
              })
            }
            placeholder="Например, 1500"
          />
        </div>

        <div className="row">
          <label className="label" htmlFor="comment">
            Комментарий
          </label>
          <textarea
            id="comment"
            className="textarea"
            value={form.comment}
            onChange={(event) =>
              setForm({
                ...form,
                comment: event.target.value,
              })
            }
            placeholder="Например, возврат остатка депозита"
          />
        </div>

        <div className="button-row">
          <button className="button" onClick={handleSubmit} disabled={submitting}>
            {submitting ? "Создание..." : requestTypeLabels[form.type]}
          </button>
        </div>
      </section>

      <section className="panel">
        <h3>История финансовых запросов</h3>

        {!overview || overview.financeRequests.length === 0 ? (
          <p className="muted">Пока нет запросов.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
              <tr>
                <th>ID</th>
                <th>Тип</th>
                <th>Сумма</th>
                <th>Статус</th>
                <th>Комментарий</th>
                <th>Создан</th>
              </tr>
              </thead>
              <tbody>
              {overview.financeRequests.map((request) => (
                <tr key={request.id}>
                  <td>{request.id}</td>
                  <td>{requestTypeLabels[request.type]}</td>
                  <td>{formatMoney(request.amount)} ₽</td>
                  <td>
                      <span className={`status-chip status-${request.status.toLowerCase()}`}>
                        {requestStatusLabels[request.status] ?? request.status}
                      </span>
                  </td>
                  <td>{request.comment || "—"}</td>
                  <td>{formatDate(request.createdAt)}</td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel">
        <h3>История операций по счету</h3>

        {!overview || overview.transactions.length === 0 ? (
          <p className="muted">Пока нет операций.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
              <tr>
                <th>ID</th>
                <th>Операция</th>
                <th>Сумма</th>
                <th>Описание</th>
                <th>Дата</th>
              </tr>
              </thead>
              <tbody>
              {overview.transactions.map((transaction) => (
                <tr key={transaction.id}>
                  <td>{transaction.id}</td>
                  <td>{transactionTypeLabels[transaction.type] ?? transaction.type}</td>
                  <td className={Number(transaction.amount) < 0 ? "amount-negative" : "amount-positive"}>
                    {formatMoney(transaction.amount)} ₽
                  </td>
                  <td>{transaction.description}</td>
                  <td>{formatDate(transaction.createdAt)}</td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
