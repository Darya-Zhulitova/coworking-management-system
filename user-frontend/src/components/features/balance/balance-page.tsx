'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { requestJson } from '@/lib/client/api';
import { formatDateTime, formatMoney } from '@/lib/format';
import { StatusBadge } from '@/components/ui/status-badge';
import { notifyCoworkingContextChanged } from '@/components/layout/coworking-shell-context';
import type { LedgerEntry, MembershipStatus, PayRequest } from '@/lib/types';

type BalanceDetails = {
  membershipId: number;
  coworkingId: number;
  membershipStatus: MembershipStatus;
  balanceMinorUnits: number;
  ledger: LedgerEntry[];
  payRequests: Array<PayRequest & { adminComment?: string | null }>;
};

type LedgerFilter = 'all' | 'money' | 'booking' | 'compensation';
type RequestDirection = 'deposit' | 'withdrawal';

function normalizeMembershipStatus(status: string): MembershipStatus {
  const normalized = status.toLowerCase();
  if (normalized === 'active' || normalized === 'pending' || normalized === 'blocked') {
    return normalized;
  }
  return 'pending';
}

function getLedgerFilterMatch(filter: LedgerFilter, entry: LedgerEntry): boolean {
  if (filter === 'all') return true;
  if (filter === 'money') return entry.type === 'DEPOSIT' || entry.type === 'WITHDRAWAL';
  if (filter === 'booking') return entry.type === 'BOOKING_CHARGE';
  return entry.type === 'CANCELLATION_REFUND'
    || entry.type === 'DAY_CLOSURE_COMPENSATION'
    || entry.type === 'MEMBERSHIP_BLOCK_COMPENSATION';
}

export function BalancePage({ coworkingId }: { coworkingId: number }) {
  const [data, setData] = useState<BalanceDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ledgerFilter, setLedgerFilter] = useState<LedgerFilter>('all');
  const [direction, setDirection] = useState<RequestDirection>('deposit');
  const [amountRubles, setAmountRubles] = useState('');
  const [comment, setComment] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadData = useCallback(async ({ silent = false }: { silent?: boolean } = {}) => {
    if (!silent) {
      setIsLoading(true);
      setError(null);
    }

    try {
      const response = await requestJson<Omit<BalanceDetails, 'membershipStatus'> & {
        membershipStatus: string
      }>(`/api/coworkings/${coworkingId}/balance`);
      setData({ ...response, membershipStatus: normalizeMembershipStatus(response.membershipStatus) });
      setError(null);
    } catch (loadError) {
      if (!silent) {
        setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить данные баланса.');
      }
    } finally {
      if (!silent) {
        setIsLoading(false);
      }
    }
  }, [coworkingId]);

  useEffect(() => {
    void loadData();

    const refreshIntervalId = window.setInterval(() => {
      void loadData({ silent: true });
    }, 5000);

    return () => {
      window.clearInterval(refreshIntervalId);
    };
  }, [loadData]);
  const filteredLedger = useMemo(() => (data?.ledger ?? []).filter((entry) => getLedgerFilterMatch(ledgerFilter, entry)), [data?.ledger, ledgerFilter]);
  const actionsLocked = data?.membershipStatus === 'pending';
  const canCreateDeposit = data?.membershipStatus === 'active';
  const canCreateWithdrawal = data?.membershipStatus === 'active' || data?.membershipStatus === 'blocked';
  const canSubmitCurrentRequest = direction === 'deposit' ? canCreateDeposit : canCreateWithdrawal;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);

    const parsedAmount = Number(amountRubles.replace(',', '.'));
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setSubmitError('Введите положительную сумму.');
      return;
    }

    const amountMinorUnits = Math.round(parsedAmount * 100) * (direction === 'withdrawal' ? -1 : 1);
    if (!comment.trim()) {
      setSubmitError('Добавьте комментарий к запросу.');
      return;
    }

    setIsSubmitting(true);
    try {
      await requestJson<{ id: number }>('/api/pay-requests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coworkingId, amount: amountMinorUnits, userComment: comment.trim() }),
      });
      setAmountRubles('');
      setComment('');
      await loadData();
      notifyCoworkingContextChanged();
    } catch (submitRequestError) {
      setSubmitError(submitRequestError instanceof Error ? submitRequestError.message : 'Не удалось создать финансовый запрос.');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return <div className="alert alert-light border">Загрузка данных баланса…</div>;
  }

  if (error || !data) {
    return <div className="alert alert-danger">{error ?? 'Не удалось загрузить данные баланса.'}</div>;
  }

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h1 className="h3">Баланс</h1>
            </div>
            <div className="text-end h3">{formatMoney(data.balanceMinorUnits)}</div>
          </div>
        </div>
      </section>

      <div className="row g-4">
        <div className="col-12 col-xl-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">История операций</h2>
                <select className="form-select form-select-sm w-auto" value={ledgerFilter}
                        onChange={(event) => setLedgerFilter(event.target.value as LedgerFilter)}>
                  <option value="all">Все типы</option>
                  <option value="money">Пополнения и вывод</option>
                  <option value="booking">Бронирования</option>
                  <option value="compensation">Компенсации и возвраты</option>
                </select>
              </div>
              <div className="table-responsive">
                <table className="table align-middle mb-0">
                  <thead>
                  <tr>
                    <th>Дата</th>
                    {/*<th>Операция</th>*/}
                    <th>Комментарий</th>
                    <th className="text-end">Сумма</th>
                  </tr>
                  </thead>
                  <tbody>
                  {filteredLedger.length > 0 ? filteredLedger.map((entry) => (
                    <tr key={entry.id}>
                      <td>{formatDateTime(entry.timestamp)}</td>
                      <td>
                        <div className="fw-semibold">{entry.name}</div>
                        {/*<div className="small text-body-secondary">{entry.type}</div>*/}
                      </td>
                      <td>{entry.comment || '—'}</td>
                      <td
                        className={`text-end fw-semibold ${entry.amount >= 0 ? 'text-success' : 'text-danger'}`}>{formatMoney(entry.amount)}</td>
                    </tr>
                  )) : (
                    <tr>
                      <td colSpan={4} className="text-center text-body-secondary py-4">Операций по выбранному фильтру
                        пока нет.
                      </td>
                    </tr>
                  )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-grid gap-4">
              <div>
                <h2 className="h5 mb-3">Новый финансовый запрос</h2>
                <form className="d-grid gap-3" onSubmit={handleSubmit}>
                  <div className="btn-group" role="group" aria-label="Тип операции">
                    <button type="button"
                            className={`btn ${direction === 'deposit' ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => setDirection('deposit')} disabled={isSubmitting}>Пополнить
                    </button>
                    <button type="button"
                            className={`btn ${direction === 'withdrawal' ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => setDirection('withdrawal')} disabled={isSubmitting}>Вывести
                    </button>
                  </div>
                  <div>
                    <label className="form-label" htmlFor="pay-request-amount">Сумма, ₽</label>
                    <input id="pay-request-amount" className="form-control" inputMode="decimal" value={amountRubles}
                           onChange={(event) => setAmountRubles(event.target.value)}
                           disabled={!canSubmitCurrentRequest || isSubmitting} placeholder="Например, 1500"/>
                  </div>
                  <div>
                    <label className="form-label" htmlFor="pay-request-comment">Комментарий</label>
                    <textarea id="pay-request-comment" className="form-control" rows={3} value={comment}
                              onChange={(event) => setComment(event.target.value)}
                              disabled={!canSubmitCurrentRequest || isSubmitting}
                              placeholder="Укажите назначение платежа или пояснение"/>
                  </div>
                  <button type="submit" className="btn btn-primary" disabled={!canSubmitCurrentRequest || isSubmitting}>
                    {isSubmitting ? 'Отправка…' : 'Создать запрос'}
                  </button>
                  {!canSubmitCurrentRequest && direction === 'deposit' &&
                      <div className="small text-body-secondary">Пополнение счета недоступно.</div>}
                  {!canSubmitCurrentRequest && direction === 'withdrawal' &&
                      <div className="small text-body-secondary">Списание со счета недоступно.</div>}
                  {submitError && <div className="alert alert-danger py-2 mb-0">{submitError}</div>}
                </form>
              </div>

              <div>
                <h2 className="h5 mb-3">Мои финансовые запросы</h2>
                <div className="d-grid gap-3">
                  {data.payRequests.length > 0 ? data.payRequests.map((request) => (
                    <div className="border rounded-4 p-3" key={request.id}>
                      <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                        <div className="fw-semibold">{formatMoney(request.amount)}</div>
                        <StatusBadge status={request.status}/>
                      </div>
                      <div className="small text-body-secondary mb-2">{formatDateTime(request.createdAt)}</div>
                      <div>{request.userComment}</div>
                      {request.adminComment ? <div className="small text-body-secondary mt-2">Комментарий
                        администратора: {request.adminComment}</div> : null}
                    </div>
                  )) : <div className="text-body-secondary small">Финансовых запросов пока нет.</div>}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
