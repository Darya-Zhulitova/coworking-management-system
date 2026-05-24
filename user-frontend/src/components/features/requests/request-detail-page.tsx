'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatDateTime, formatMoney } from '@/lib/format';
import { StatusBadge } from '@/components/ui/status-badge';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { BackendServiceRequest, BackendServiceRequestMessage } from '@/lib/api/backend';
import type { MessageAuthorType, ServiceRequest } from '@/lib/types';
import { useToast } from '@/components/ui/toast-provider';
import { LoadingOverlay } from '@/components/ui/loading-overlay';

function bubbleClass(authorType: MessageAuthorType): string {
  if (authorType === 'USER') return 'bg-primary-subtle';
  if (authorType === 'ADMIN') return 'bg-body-tertiary';
  return 'bg-warning-subtle';
}

function normalizeStatus(value: BackendServiceRequest['status']): ServiceRequest['status'] {
  return String(value).toLowerCase() as ServiceRequest['status'];
}

export function RequestDetailPage({
                                    membershipId,
                                    requestId,
                                    initialRequest,
                                    initialMessages,
                                    initialMembershipStatus,
                                    initialError = null
                                  }: {
  membershipId: number;
  requestId: number;
  initialRequest: BackendServiceRequest | null;
  initialMessages: BackendServiceRequestMessage[];
  initialMembershipStatus: 'active' | 'pending' | 'blocked' | null;
  initialError?: string | null
}) {
  const toast = useToast();
  const [request, setRequest] = useState<ServiceRequest | null>(initialRequest ? {
    id: initialRequest.id,
    membershipId: initialRequest.membershipId,
    typeId: initialRequest.typeId,
    name: initialRequest.name,
    typeName: initialRequest.typeName,
    cost: initialRequest.cost,
    status: normalizeStatus(initialRequest.status),
    createdAt: initialRequest.createdAt,
    updatedAt: initialRequest.updatedAt,
    resolvedAt: initialRequest.resolvedAt ?? undefined,
  } : null);
  const [messages, setMessages] = useState<BackendServiceRequestMessage[]>(initialMessages);
  const [membershipStatus, setMembershipStatus] = useState<'active' | 'pending' | 'blocked' | null>(initialMembershipStatus);
  const [messageText, setMessageText] = useState('');
  const [messageFile, setMessageFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(initialError);

  const loadData = useCallback(async ({ silent = false }: { silent?: boolean } = {}) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }

    try {
      const [requestItem, requestMessages, context] = await Promise.all([
        requestJson<BackendServiceRequest>(`/api/memberships/${membershipId}/service-requests/${requestId}`),
        requestJson<BackendServiceRequestMessage[]>(`/api/memberships/${membershipId}/service-requests/${requestId}/messages`),
        requestJson<{
          membership: { status: 'active' | 'pending' | 'blocked' | null }
        }>(`/api/memberships/${membershipId}/context`),
      ]);

      setRequest({
        id: requestItem.id,
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
      setMembershipStatus(context.membership.status);
      setError(null);
    } catch (err: unknown) {
      if (!silent) {
        setError(err instanceof ClientRequestError ? err.message : 'Не удалось загрузить сервисную заявку.');
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, [membershipId, requestId]);

  useEffect(() => {
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
    if (!canReply || (!messageText.trim() && !messageFile)) return;

    setSubmitting(true);
    setError(null);
    try {
      const formData = new FormData();
      if (messageText.trim()) formData.set('text', messageText.trim());
      if (messageFile) formData.set('file', messageFile);
      const created = await requestJson<BackendServiceRequestMessage>(`/api/memberships/${membershipId}/service-requests/${requestId}/messages`, {
        method: 'POST',
        body: formData,
      });
      setMessages((current) => [...current, created]);
      setMessageText('');
      setMessageFile(null);
      toast.success('Сообщение отправлено.');
    } catch (err: unknown) {
      toast.error(err instanceof ClientRequestError ? err.message : 'Не удалось отправить сообщение.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <LoadingOverlay message="Загрузка данных..." ariaLabel="Загрузка данных"/>;
  if (error && !request) return <div
    className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 mb-0">{error}</div>;
  if (!request) return <div
    className="border rounded-4 border-warning-subtle bg-warning-subtle text-warning-emphasis p-3 mb-0">Сервисная заявка
    не найдена.</div>;

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
                <div className="small text-body-secondary mt-2">Оплата произойдет только при закрытии сервисной
                  заявки.
                </div>
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
                  {message.text ? <div>{message.text}</div> : null}
                  {message.attachments?.length ?
                    <div className="d-grid gap-2 mt-2">{message.attachments.map((attachment) => <a key={attachment.id}
                                                                                                   href={attachment.url}
                                                                                                   target="_blank"
                                                                                                   rel="noreferrer"
                                                                                                   className="btn btn-sm btn-outline-secondary text-start">📎 {attachment.fileName}</a>)}</div> : null}
                  {message.readAt &&
                      <div className="small text-body-secondary mt-2">Прочитано {formatDateTime(message.readAt)}</div>}
                </div>
              ))}
            </div>
            <form className="d-grid gap-3" onSubmit={handleSubmit}>
              <textarea className="form-control" rows={4} placeholder="Введите сообщение" value={messageText}
                        onChange={(event) => setMessageText(event.target.value)} disabled={!canReply || submitting}
                        maxLength={1000}/>
              <input className="form-control" type="file" disabled={!canReply || submitting}
                     onChange={(event) => setMessageFile(event.target.files?.[0] ?? null)}/>
              <div className="form-text">Можно прикрепить файл любого формата размером до 50 МБ.</div>
              {messageFile ? <div className="small text-body-secondary">Файл: {messageFile.name}</div> : null}
              {membershipStatus === 'pending' &&
                  <div className="border rounded-4 bg-body-secondary p-3 mb-0">Отправка сообщений доступна только после
                      подтверждения
                      доступа к коворкингу.</div>}
              <div className="d-flex justify-content-end">
                <button type="submit" className="btn btn-primary"
                        disabled={!canReply || submitting || (!messageText.trim() && !messageFile)}>{submitting ? 'Отправка...' : 'Отправить сообщение'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
