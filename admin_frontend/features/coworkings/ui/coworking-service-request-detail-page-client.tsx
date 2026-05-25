'use client';

import { FormEvent, useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { ServiceRequestDetailDto, ServiceRequestMessageDto } from '@/types/place';
import { formatRublesFromKopecks } from '@/lib/format/money';
import { formatMessageAuthorType, formatServiceRequestStatus } from '@/lib/format/labels';

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function CoworkingServiceRequestDetailPageClient({ coworkingId, serviceRequestId }: {
  coworkingId: number;
  serviceRequestId: number
}) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [detail, setDetail] = useState<ServiceRequestDetailDto | null>(null);
  const [messages, setMessages] = useState<ServiceRequestMessageDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [messageText, setMessageText] = useState('');
  const [messageFile, setMessageFile] = useState<File | null>(null);
  const [submittingMessage, setSubmittingMessage] = useState(false);
  const [changingStatus, setChangingStatus] = useState<string | null>(null);

  const canEdit = context?.grants.includes('USER_EDIT') ?? false;

  async function loadDetail() {
    const data = await requestJson<ServiceRequestDetailDto>(`/api/coworkings/${coworkingId}/users/service-requests/${serviceRequestId}`);
    setDetail(data);
    return data;
  }

  async function loadMessages() {
    const data = await requestJson<ServiceRequestMessageDto[]>(`/api/coworkings/${coworkingId}/users/service-requests/${serviceRequestId}/messages`);
    setMessages(data);
    return data;
  }

  useEffect(() => {
    let mounted = true;
    Promise.all([loadDetail(), loadMessages()])
      .catch((err: unknown) => {
        if (!mounted) return;
        setError(err instanceof ClientRequestError ? err.message : 'Не удалось загрузить сервисную заявку.');
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
          setMessagesLoading(false);
        }
      });
    return () => {
      mounted = false;
    };
  }, [coworkingId, serviceRequestId]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      loadMessages()
        .catch(() => undefined)
        .finally(() => setMessagesLoading(false));
    }, 10000);
    return () => window.clearInterval(intervalId);
  }, [coworkingId, serviceRequestId]);

  async function handleStatusChange(status: 'in_progress' | 'resolved' | 'rejected') {
    setActionError(null);
    setActionMessage(null);
    setChangingStatus(status);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/users/service-requests/${serviceRequestId}/status/${status}`, { method: 'POST' });
      await Promise.all([loadDetail(), loadMessages()]);
      setActionMessage(
        status === 'in_progress'
          ? 'Сервисная заявка принята в работу.'
          : status === 'resolved'
            ? 'Сервисная заявка закрыта.'
            : 'Сервисная заявка отклонена.'
      );
    } catch (err) {
      setActionError(err instanceof ClientRequestError ? err.message : 'Не удалось изменить статус сервисной заявки.');
    } finally {
      setChangingStatus(null);
    }
  }

  async function handleSendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!messageText.trim() && !messageFile) return;
    setActionError(null);
    setActionMessage(null);
    setSubmittingMessage(true);
    try {
      const formData = new FormData();
      if (messageText.trim()) formData.set('text', messageText.trim());
      if (messageFile) formData.set('file', messageFile);
      await requestJson(`/api/coworkings/${coworkingId}/users/service-requests/${serviceRequestId}/messages`, {
        method: 'POST',
        body: formData,
      });
      setMessageText('');
      setMessageFile(null);
      await Promise.all([loadDetail(), loadMessages()]);
      setActionMessage('Сообщение отправлено.');
    } catch (err) {
      setActionError(err instanceof ClientRequestError ? err.message : 'Не удалось отправить сообщение.');
    } finally {
      setSubmittingMessage(false);
    }
  }

  if (contextLoading || loading) return <FullPageLoader label="Загрузка сервисной заявки..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить сервисную заявку.'}/>;
  if (!context || context.coworkingId == null || !detail) return <FullPageLoader label="Переход на страницу входа..."/>;

  const isFinal = detail.status === 'resolved' || detail.status === 'rejected';
  const canTakeInWork = canEdit && detail.status === 'new';
  const canReject = canEdit && !isFinal;
  const insufficientBalance = detail.cost > 0 && detail.balanceMinorUnits < detail.cost;
  const canClose = canEdit && !isFinal && !insufficientBalance;

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <div><h2 className="mb-1">{detail.name}</h2>
      <div className="text-body-secondary">{detail.typeName}</div>
    </div>
    <Card className="content-card"><Card.Body><Stack
      gap={3}>
      <div className="d-flex flex-wrap justify-content-end gap-3">
        <div className="text-end">
          <div className="small text-body-secondary">Статус</div>
          <div className="fw-semibold">{formatServiceRequestStatus(detail.status)}</div>
        </div>
      </div>
      {actionMessage ? <Alert variant="success" className="mb-0">{actionMessage}</Alert> : null}{actionError ?
      <Alert variant="danger" className="mb-0">{actionError}</Alert> : null}{insufficientBalance && !isFinal ?
      <Alert variant="warning" className="mb-0">Эту платную сервисную заявку сейчас нельзя закрыть, потому что у
        пользователя
        недостаточно средств.</Alert> : null}<Row className="g-4"><Col lg={6}><Card className="h-100"><Card.Body><Stack
      gap={2}>
      <div><span className="text-body-secondary">Пользователь:</span> {detail.userName}</div>
      <div><span className="text-body-secondary">Email:</span> {detail.userEmail}</div>
      <div><span className="text-body-secondary">Участие:</span> #{detail.membershipId}</div>
      <div><span className="text-body-secondary">Создана:</span> {formatDateTime(detail.createdAt)}</div>
      <div><span className="text-body-secondary">Обновлена:</span> {formatDateTime(detail.updatedAt)}</div>
      {detail.resolvedAt ?
        <div><span className="text-body-secondary">Закрыта:</span> {formatDateTime(detail.resolvedAt)}
        </div> : null}{detail.cost > 0 ?
      <div><span className="text-body-secondary">Стоимость:</span> {formatRublesFromKopecks(detail.cost)}</div> : null}
      <div><span
        className="text-body-secondary">Баланс пользователя:</span> {formatRublesFromKopecks(detail.balanceMinorUnits)}
      </div>
    </Stack></Card.Body></Card></Col><Col lg={6}><Card className="h-100"><Card.Body><Stack gap={3}>
      <div className="text-body-secondary">Действия</div>
      <div className="d-flex flex-wrap gap-2">{canTakeInWork ?
        <Button variant="outline-primary" disabled={changingStatus !== null}
                onClick={() => handleStatusChange('in_progress')}>Взять в работу</Button> : null}{canClose ?
        <Button variant="outline-primary" disabled={changingStatus !== null}
                onClick={() => handleStatusChange('resolved')}>Закрыть</Button> :
        <Button variant="outline-primary" disabled>Закрыть</Button>}{canReject ?
        <Button variant="outline-primary" disabled={changingStatus !== null}
                onClick={() => handleStatusChange('rejected')}>Отклонить</Button> : null}</div>
      {isFinal ? <Alert variant="secondary" className="mb-0">Финальные сервисные заявки доступны только для
        просмотра.</Alert> : null}{!canEdit ?
      <Alert variant="secondary" className="mb-0">У вас нет прав на изменение.</Alert> : null}
    </Stack></Card.Body></Card></Col></Row></Stack></Card.Body></Card><Card
    className="content-card"><Card.Body><Stack gap={3}>
    <div className="d-flex justify-content-between align-items-center gap-3"><Card.Title as="h2"
                                                                                         className="mb-0">Сообщения</Card.Title>
    </div>
    <div className="d-grid gap-3">{messages.length === 0 ?
      <div className="text-body-secondary">Сообщений пока нет.</div> : messages.map((message) => <div
        key={message.id} className="border rounded-4 p-3">
        <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
          <div className="fw-semibold">{message.authorName || formatMessageAuthorType(message.authorType)}</div>
          <div className="small text-body-secondary">{formatDateTime(message.createdAt)}</div>
        </div>
        {message.text ? <div>{message.text}</div> : null}
        {message.attachments?.length ?
          <div className="d-grid gap-2 mt-2">{message.attachments.map((attachment) => <a key={attachment.id}
                                                                                         href={attachment.url}
                                                                                         target="_blank"
                                                                                         rel="noreferrer"
                                                                                         className="btn btn-sm btn-outline-secondary text-start">📎 {attachment.fileName}</a>)}</div> : null}
      </div>)}</div>
    <Form onSubmit={handleSendMessage}><Stack gap={2}><Form.Control as="textarea" rows={3} value={messageText}
                                                                    onChange={(event) => setMessageText(event.target.value)}
                                                                    placeholder="Введите сообщение пользователю"
                                                                    disabled={!canEdit || isFinal || submittingMessage}/>
      <Form.Control type="file" disabled={!canEdit || isFinal || submittingMessage}
                    onChange={(event) => setMessageFile(event.currentTarget.files?.[0] ?? null)}/>
      <div className="d-flex justify-content-end"><Button type="submit"
                                                          disabled={!canEdit || isFinal || submittingMessage || (!messageText.trim() && !messageFile)}>{submittingMessage ? 'Отправка...' : 'Отправить сообщение'}</Button>
      </div>
    </Stack></Form></Stack></Card.Body></Card></Stack></Container></main>;
}
