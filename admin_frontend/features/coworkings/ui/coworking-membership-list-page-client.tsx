'use client';

import { useCallback, useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import { formatRublesFromKopecks } from '@/lib/format/money';
import type { MembershipListItemDto } from '@/types/place';

function statusLabel(value: string): string {
  const labels: Record<string, string> = {
    ACTIVE: 'Активный',
    PENDING: 'Ожидает подтверждения',
    BLOCKED: 'Заблокирован',
  };
  return labels[value] ?? value;
}

function statusVariant(value: string): string {
  if (value === 'ACTIVE') return 'success';
  if (value === 'PENDING') return 'warning';
  if (value === 'BLOCKED') return 'danger';
  return 'secondary';
}

function formatDate(value?: string | null): string {
  return value ? value.slice(0, 10) : '—';
}

export function CoworkingMembershipListPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true,
  });
  const [memberships, setMemberships] = useState<MembershipListItemDto[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (query: string) => {
    const suffix = query.trim() ? `?search=${encodeURIComponent(query.trim())}` : '';
    const data = await requestJson<MembershipListItemDto[]>(`/api/coworkings/${coworkingId}/memberships${suffix}`);
    setMemberships(data);
  }, [coworkingId]);

  useEffect(() => {
    let mounted = true;
    const timeoutId = window.setTimeout(() => {
      load(search)
        .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить пользователей.'))
        .finally(() => mounted && setLoading(false));
    }, 250);
    return () => {
      mounted = false;
      window.clearTimeout(timeoutId);
    };
  }, [load, search]);

  if (contextLoading || loading) return <FullPageLoader label="Загрузка пользователей..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить пользователей.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return <main className="page-shell">
    <Container className="py-4 py-md-5">
      <Stack gap={4}>
        <Card className="content-card">
          <Card.Body>
            <Stack direction="horizontal" gap={3} className="flex-wrap justify-content-between align-items-start">
              <div>
                <h1 className="h3 mb-2">Пользователи коворкинга</h1>
                <p className="mb-0 text-body-secondary">Поиск резидентов, просмотр профиля, баланса и бронирований.</p>
              </div>
            </Stack>
          </Card.Body>
        </Card>

        <Card className="content-card">
          <Card.Body>
            <Form.Control
              className="mb-3"
              placeholder="Поиск по имени или email"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            {memberships.length === 0 ? <Alert variant="secondary" className="mb-0">Пользователи не найдены.</Alert> :
              <Table responsive hover>
                <thead>
                <tr>
                  <th>Пользователь</th>
                  <th>Статус</th>
                  <th>Дата создания</th>
                  <th>Баланс</th>
                  <th>Брони</th>
                  <th className="text-end">Действия</th>
                </tr>
                </thead>
                <tbody>
                {memberships.map((membership) => <tr key={membership.membershipId}>
                  <td>
                    {membership.userName}<br/>
                    <span
                      className="text-body-secondary">{membership.userEmail ?? `пользователь #${membership.userId}`}</span>
                  </td>
                  <td><Badge bg={statusVariant(membership.status)}>{statusLabel(membership.status)}</Badge></td>
                  <td>{formatDate(membership.createdAt)}</td>
                  <td>{formatRublesFromKopecks(membership.balanceMinorUnits)}</td>
                  <td>{membership.activeBookingsCount}</td>
                  <td className="text-end">
                    <Button size="sm"
                            href={`/coworkings/${coworkingId}/users/membership-list/${membership.membershipId}`}>Открыть
                      профиль</Button>
                  </td>
                </tr>)}
                </tbody>
              </Table>}
          </Card.Body>
        </Card>
      </Stack>
    </Container>
  </main>;
}
