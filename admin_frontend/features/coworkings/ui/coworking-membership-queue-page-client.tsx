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
import type { MembershipQueueItemDto } from '@/types/place';
import { formatMembershipStatus } from '@/lib/format/labels';

export function CoworkingMembershipQueuePageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [items, setItems] = useState<MembershipQueueItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const canEdit = context?.grants.includes('USER_EDIT') ?? false;

  async function load() {
    setItems(await requestJson<MembershipQueueItemDto[]>(`/api/coworkings/${coworkingId}/users/memberships`));
  }

  useEffect(() => {
    let mounted = true;
    load().catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить очередь заявок на участие.')).finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [coworkingId]);

  async function act(id: number, action: 'approve' | 'reject') {
    await requestJson(`/api/coworkings/${coworkingId}/users/memberships/${id}/${action}`, { method: 'POST' });
    await load();
    setMessage(action === 'approve' ? 'Заявка на участие подтверждена.' : 'Заявка на участие отклонена.');
  }

  if (contextLoading || loading) return <FullPageLoader label="Загрузка очереди заявок..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить очередь заявок на участие.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}><h2 className="mb-0">Очередь
    заявок на участие</h2>{message ?
    <Alert variant="success">{message}</Alert> : null}<Card className="content-card"><Card.Body><Card.Text
    className="text-body-secondary">Управляйте заявками и статусами
    пользователей из единой очереди.</Card.Text><Table responsive hover className="mt-3">
    <thead>
    <tr>
      <th>Пользователь</th>
      <th>Статус</th>
      <th>Создано</th>
      <th>Действия</th>
    </tr>
    </thead>
    <tbody>{items.map((item) => <tr key={item.membershipId}>
      <td>{item.userName}</td>
      <td>{formatMembershipStatus(item.status)}</td>
      <td>{item.createdAt}</td>
      <td>{canEdit && item.status === 'pending' ? <Stack direction="horizontal" gap={2}><Button size="sm"
                                                                                                onClick={() => act(item.membershipId, 'approve')}>Подтвердить</Button><Button
        size="sm" variant="outline-danger"
        onClick={() => act(item.membershipId, 'reject')}>Отклонить</Button></Stack> : '—'}</td>
    </tr>)}</tbody>
  </Table></Card.Body></Card></Stack></Container></main>;
}
