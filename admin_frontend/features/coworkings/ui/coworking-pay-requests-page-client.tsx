'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { PayRequestQueueItemDto } from '@/types/place';
import { formatRublesFromKopecks } from '@/lib/format/money';
import { formatPayRequestStatus } from '@/lib/format/labels';

export function CoworkingPayRequestsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [items, setItems] = useState<PayRequestQueueItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const canEdit = context?.grants.includes('USER_EDIT') ?? false;

  async function load() {
    setItems(await requestJson<PayRequestQueueItemDto[]>(`/api/coworkings/${coworkingId}/users/pay-requests`));
  }

  useEffect(() => {
    let mounted = true;
    load().catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить финансовые запросы.')).finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [coworkingId]);

  async function act(id: number, action: 'approve' | 'reject') {
    await requestJson(`/api/coworkings/${coworkingId}/users/pay-requests/${id}/${action}`, { method: 'POST' });
    await load();
    setMessage(action === 'approve' ? 'Финансовый запрос подтверждён.' : 'Финансовый запрос отклонён.');
  }

  if (contextLoading || loading) return <FullPageLoader label="Загрузка финансовых запросов..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить финансовые запросы.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}><h2 className="mb-0">Финансовые
    запросы</h2>{message ?
    <Alert variant="success">{message}</Alert> : null}<Card className="content-card"><Card.Body><Table responsive hover
                                                                                                       className="mt-3">
    <thead>
    <tr>
      <th>Пользователь</th>
      <th>Сумма</th>
      <th>Статус</th>
      <th>Комментарий пользователя</th>
      <th>Действия</th>
    </tr>
    </thead>
    <tbody>{items.map((item) => <tr key={item.payRequestId}>
      <td>{item.userName}</td>
      <td>{formatRublesFromKopecks(item.amount)}</td>
      <td>{formatPayRequestStatus(item.status)}</td>
      <td>{item.userComment}</td>
      <td>{canEdit && item.status === 'PENDING' ? <Stack direction="horizontal" gap={2}><Button size="sm"
                                                                                                onClick={() => act(item.payRequestId, 'approve')}>Подтвердить</Button><Button
        size="sm" variant="outline-danger"
        onClick={() => act(item.payRequestId, 'reject')}>Отклонить</Button></Stack> : (item.adminComment ?? '—')}</td>
    </tr>)}</tbody>
  </Table></Card.Body></Card></Stack></Container></main>;
}
