"use client";

import {useEffect, useMemo, useState} from "react";
import {createFinanceRequest, getBalanceOverview} from "@/lib/api";
import type {BalanceOverview, CreateFinanceRequestPayload, FinanceRequestType} from "@/lib/types";

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

function getStatusBadgeClass(status: string) {
  if (status === "COMPLETED") {
    return "text-bg-success";
  }

  if (status === "REJECTED") {
    return "text-bg-danger";
  }

  return "text-bg-warning";
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
    } catch {
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
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Не удалось создать финансовый запрос.");
    } finally {
      setSubmitting(false);
    }
  }

  useEffect(() => {
    void loadOverview();
  }, []);

  return (
    <div className="d-grid gap-4">
      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
            <div>
              <h3 className="h5 mb-1">Состояние счета</h3>
              <p className="text-body-secondary mb-0">Актуальный баланс, доступный остаток и статус заявок.</p>
            </div>
            <button className="btn btn-outline-primary" onClick={loadOverview} disabled={loading}>
              {loading ? "Загрузка..." : "Обновить"}
            </button>
          </div>

          {overview ? (
            <div className="row g-3">
              <div className="col-sm-6 col-xl-3">
                <article className="border rounded-3 p-3 h-100 bg-body-tertiary">
                  <span className="d-block text-body-secondary small mb-2">Текущий баланс</span>
                  <strong className="fs-4">{formatMoney(overview.account.balance)} ₽</strong>
                </article>
              </div>
              <div className="col-sm-6 col-xl-3">
                <article className="border rounded-3 p-3 h-100 bg-body-tertiary">
                  <span className="d-block text-body-secondary small mb-2">Доступно сейчас</span>
                  <strong className="fs-4">
                    {formatMoney(String(Number(overview.account.balance) - pendingWithdrawTotal))} ₽
                  </strong>
                </article>
              </div>
              <div className="col-sm-6 col-xl-3">
                <article className="border rounded-3 p-3 h-100 bg-body-tertiary">
                  <span className="d-block text-body-secondary small mb-2">Запросов в ожидании</span>
                  <strong className="fs-4">{overview.pendingCount}</strong>
                </article>
              </div>
              <div className="col-sm-6 col-xl-3">
                <article className="border rounded-3 p-3 h-100 bg-body-tertiary">
                  <span className="d-block text-body-secondary small mb-2">Membership</span>
                  <strong className="fs-4">#{overview.account.membershipId}</strong>
                </article>
              </div>
            </div>
          ) : (
            <p className="text-body-secondary mb-0">Нет данных по счету.</p>
          )}

          {message ? <div className="alert alert-success mt-4 mb-0">{message}</div> : null}
          {error ? <div className="alert alert-danger mt-4 mb-0">{error}</div> : null}
        </div>
      </section>

      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <h3 className="h5 mb-4">Создать финансовый запрос</h3>

          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label" htmlFor="membershipId-balance">
                Membership ID
              </label>
              <input
                id="membershipId-balance"
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
              <label className="form-label" htmlFor="requestType">
                Тип запроса
              </label>
              <select
                id="requestType"
                className="form-select"
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

            <div className="col-md-4">
              <label className="form-label" htmlFor="amount">
                Сумма
              </label>
              <input
                id="amount"
                className="form-control"
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

            <div className="col-12">
              <label className="form-label" htmlFor="comment">
                Комментарий
              </label>
              <textarea
                id="comment"
                className="form-control"
                rows={4}
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
          </div>

          <div className="d-flex flex-wrap gap-2 mt-4">
            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
              {submitting ? "Создание..." : requestTypeLabels[form.type]}
            </button>
          </div>
        </div>
      </section>

      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <h3 className="h5 mb-4">История финансовых запросов</h3>

          {!overview || overview.financeRequests.length === 0 ? (
            <p className="text-body-secondary mb-0">Пока нет запросов.</p>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle mb-0">
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
                        <span className={`badge ${getStatusBadgeClass(request.status)}`}>
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
        </div>
      </section>

      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <h3 className="h5 mb-4">История операций по счету</h3>

          {!overview || overview.transactions.length === 0 ? (
            <p className="text-body-secondary mb-0">Пока нет операций.</p>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle mb-0">
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
                      <td className={Number(transaction.amount) < 0 ? "text-danger fw-semibold" : "text-success fw-semibold"}>
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
        </div>
      </section>
    </div>
  );
}
