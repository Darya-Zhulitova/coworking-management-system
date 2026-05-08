'use client';

import { useEffect, useState } from 'react';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { CoworkingUserReadModelDto } from '@/types/place';
import { formatRublesFromKopecks } from '@/lib/format/money';

export function CoworkingUsersListPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [users, setUsers] = useState<CoworkingUserReadModelDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    let mounted = true;
    requestJson<CoworkingUserReadModelDto[]>(`/api/coworkings/${coworkingId}/users`).then((data) => mounted && setUsers(data)).catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить пользователей.')).finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [coworkingId]);
  if (contextLoading || loading) return <FullPageLoader label="Загрузка списка пользователей..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить пользователей.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}><h2 className="mb-0">Список
    пользователей</h2><Card className="content-card"><Card.Body><Table responsive hover className="mt-3">
    <thead>
    <tr>
      <th>Название</th>
      <th>Дата регистрации</th>
      <th>Баланс</th>
      <th>Всего бронирований</th>
      <th>Активные бронирования</th>
    </tr>
    </thead>
    <tbody>{users.map((user) => <tr key={user.userId}>
      <td>{user.name}</td>
      <td>{user.registeredAt}</td>
      <td>{formatRublesFromKopecks(user.balance)}</td>
      <td>{user.totalBookings}</td>
      <td>{user.unfinishedBookings}</td>
    </tr>)}</tbody>
  </Table></Card.Body></Card></Stack></Container></main>;
}
