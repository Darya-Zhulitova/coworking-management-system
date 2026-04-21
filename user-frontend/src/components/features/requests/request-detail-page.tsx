'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatDateTime, formatMoney } from '@/lib/format';
import { StatusBadge } from '@/components/ui/status-badge';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { BackendBalanceDetails, BackendServiceRequest, BackendServiceRequestMessage } from '@/lib/api/backend';
import type { MessageAuthorType, ServiceRequest } from '@/lib/types';

function bubbleClass(authorType: MessageAuthorType): string {
  if (authorType === 'USER') return 'bg-primary-subtle';
  if (authorType === 'ADMIN') return 'bg-body-tertiary';
  return 'bg-warning-subtle';
}

function normalizeStatus(value: BackendServiceRequest['status']): ServiceRequest['status'] {
  return String(value).toLowerCase() as ServiceRequest['status'];
}

export function RequestDetailPage({ coworkingId, requestId }: { coworkingId: number; requestId: number }) {
  const [request, setRequest] = useState<ServiceRequest | null>(null);
  const [messages, setMessages] = useState<BackendServiceRequestMessage[]>([]);
  const [membershipStatus, setMembershipStatus] = useState<'active' | 'pending' | 'blocked' | null>(null);
  const [messageText, setMessageText] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async ({ silent = false }: { silent?: boolean } = {}) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }

    try {
      const [requestItem, requestMessages, balance] = await Promise.all([
        requestJson<BackendServiceRequest>(`/api/coworkings/${coworkingId}/service-requests/${requestId}`),
        requestJson<BackendServiceRequestMessage[]>(`/api/coworkings/${coworkingId}/service-requests/${requestId}/messages`),
        requestJson<BackendBalanceDetails>(`/api/coworkings/${coworkingId}/balance`),
      ]);

      setRequest({
        id: requestItem.id,
        coworkingId: requestItem.coworkingId,
        membershipId: requestItem.membershipId,
        typeId: requestItem.typeId,
        name: requestItem.name,
        typeName: requestItem.typeName,
        cost: requestItem.cost,
        status: normalizeStatus(requestItem.status),
        createdAt: requestItem.createdAt,
        updatedAt: requestItem.updatedAt,
        resolvedAt: requestItem.resolvedAt ?? undefined,
      });
      setMessages(requestMessages);
      setMembershipStatus(String(balance.membershipStatus).toLowerCase() as 'active' | 'pending' | 'blocked');
      setError(null);
    } catch (err: unknown) {
      if (!silent) {
        setError(err instanceof ClientRequestError ? err.message : 'Не удалось загрузить заявку.');
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, [coworkingId, requestId]);

  useEffect(() => {
    void loadData();

    const refreshIntervalId = window.setInterval(() => {
      void loadData({ silent: true });
    }, 5000);

    return () => {
      window.clearInterval(refreshIntervalId);
    };
  }, [loadData]);

  const canReply = useMemo(() => request != null && request.status !== 'resolved' && request.status !== 'rejected' && membershipStatus === 'active', [membershipStatus, request]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canReply || !messageText.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      const created = await requestJson<BackendServiceRequestMessage>(`/api/service-requests/${requestId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coworkingId, text: messageText.trim() }),
      });
      setMessages((current) => [...current, created]);
      setMessageText('');
    } catch (err: unknown) {
      setError(err instanceof ClientRequestError ? err.message : 'Не удалось отправить сообщение.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="alert alert-light border mb-0">Загрузка заявки...</div>;
  if (error && !request) return <div className="alert alert-danger mb-0">{error}</div>;
  if (!request) return <div className="alert alert-warning mb-0">Заявка не найдена.</div>;

  return (
    <div className="row g-4">
      <div className="col-12 col-xl-4">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body p-4 d-grid gap-4">
            <div>
              <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                <h1 className="h4 mb-0">{request.name}</h1>
                <StatusBadge status={request.status}/>
              </div>
              <div className="text-body-secondary small">{request.typeName}</div>
            </div>
            <div className="border rounded-4 p-3">
              <div className="text-body-secondary small">Создано</div>
              <div className="fw-semibold">{formatDateTime(request.createdAt)}</div>
            </div>
            {request.cost !== 0 && (
              <div className="border rounded-4 p-3">
                <div className="text-body-secondary small">Стоимость</div>
                <div className="fw-semibold">{formatMoney(request.cost)}</div>
                <div className="small text-body-secondary mt-2">Оплата произойдёт только при закрытии заявки.</div>
              </div>
            )}
          </div>
        </div>
      </div>
      <div className="col-12 col-xl-8">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body p-4 d-grid gap-4">
            <div className="d-grid gap-3">
              {messages.length === 0 ? (
                <div className="text-body-secondary">Сообщений пока нет.</div>
              ) : messages.map((message) => (
                <div key={message.id} className={`border rounded-4 p-3 ${bubbleClass(message.authorType)}`}>
                  <div className="d-flex justify-content-between gap-3 mb-2">
                    <div className="fw-semibold">{message.authorName}</div>
                    <div className="small text-body-secondary">{formatDateTime(message.timestamp)}</div>
                  </div>
                  <div>{message.text}</div>
                  {message.readAt &&
                      <div className="small text-body-secondary mt-2">Прочитано {formatDateTime(message.readAt)}</div>}
                </div>
              ))}
            </div>
            <form className="d-grid gap-3" onSubmit={handleSubmit}>
              <textarea className="form-control" rows={4} placeholder="Введите сообщение" value={messageText}
                        onChange={(event) => setMessageText(event.target.value)} disabled={!canReply || submitting}
                        maxLength={1000}/>
              {membershipStatus === 'pending' &&
                  <div className="alert alert-secondary mb-0">Отправка сообщений доступна только после подтверждения
                      заявки.</div>}
              {error && <div className="alert alert-danger mb-0">{error}</div>}
              <div className="d-flex justify-content-end">
                <button type="submit" className="btn btn-primary"
                        disabled={!canReply || submitting || !messageText.trim()}>{submitting ? 'Отправка...' : 'Отправить сообщение'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
