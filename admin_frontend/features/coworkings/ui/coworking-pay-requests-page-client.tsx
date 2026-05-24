'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { PayRequestQueueItemDto } from '@/types/place';
import { formatRublesFromKopecks } from '@/lib/format/money';
import { formatPayRequestStatus } from '@/lib/format/labels';

type PayRequestAction = 'approve' | 'reject';

export function CoworkingPayRequestsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [items, setItems] = useState<PayRequestQueueItemDto[]>([]);
  const [comments, setComments] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const canEdit = context?.grants.includes('USER_EDIT') ?? false;

  async function load() {
    setItems(await requestJson<PayRequestQueueItemDto[]>(`/api/coworkings/${coworkingId}/users/pay-requests`));
  }

  useEffect(() => {
    let mounted = true;
    load()
      .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить платежные заявки.'))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [coworkingId]);

  function updateComment(id: number, value: string) {
    setComments((current) => ({ ...current, [id]: value }));
  }

  async function act(id: number, action: PayRequestAction) {
    const comment = comments[id]?.trim() ?? '';
    if (action === 'reject' && comment.length === 0) {
      setError('Для отклонения платежной заявки укажите комментарий администратора.');
      return;
    }
    setError(null);
    setMessage(null);
    setProcessingId(id);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/users/pay-requests/${id}/${action}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ comment: comment.length > 0 ? comment : null })
      });
      await load();
      setComments((current) => {
        const next = { ...current };
        delete next[id];
        return next;
      });
      setMessage(action === 'approve' ? 'Платежная заявка подтверждена.' : 'Платежная заявка отклонена.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось обновить платежную заявку.');
    } finally {
      setProcessingId(null);
    }
  }

  if (contextLoading || loading) return <FullPageLoader label="Загрузка платежных заявок..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <h2 className="mb-0">Платежные заявки</h2>
    {message ? <Alert variant="success">{message}</Alert> : null}
    {error ? <Alert variant="danger">{error}</Alert> : null}
    <Card className="content-card"><Card.Body><Table responsive hover className="mt-3 align-middle">
      <thead>
      <tr>
        <th>Пользователь</th>
        <th>Сумма</th>
        <th>Статус</th>
        <th>Комментарий пользователя</th>
        <th>Комментарий администратора</th>
        <th>Действия</th>
      </tr>
      </thead>
      <tbody>{items.map((item) => {
        const isPending = item.status === 'PENDING';
        const isProcessing = processingId === item.payRequestId;
        return <tr key={item.payRequestId}>
          <td>{item.userName}</td>
          <td>{formatRublesFromKopecks(item.amount)}</td>
          <td>{formatPayRequestStatus(item.status)}</td>
          <td>{item.userComment}</td>
          <td>{isPending && canEdit ? <Form.Control
            as="textarea"
            rows={2}
            value={comments[item.payRequestId] ?? ''}
            onChange={(event) => updateComment(item.payRequestId, event.currentTarget.value)}
            placeholder="Комментарий для пользователя"
            disabled={isProcessing}
          /> : (item.adminComment || '—')}</td>
          <td>{canEdit && isPending ? <Stack direction="horizontal" gap={2}><Button
            size="sm"
            disabled={isProcessing}
            onClick={() => act(item.payRequestId, 'approve')}
          >Подтвердить</Button><Button
            size="sm"
            variant="outline-danger"
            disabled={isProcessing}
            onClick={() => act(item.payRequestId, 'reject')}
          >Отклонить</Button></Stack> : '—'}</td>
        </tr>;
      })}</tbody>
    </Table></Card.Body></Card>
  </Stack></Container></main>;
}
