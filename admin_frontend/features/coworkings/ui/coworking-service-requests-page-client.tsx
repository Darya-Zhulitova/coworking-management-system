'use client';

import { useEffect, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { ServiceRequestQueueItemDto } from '@/types/place';
import { formatOptionalRublesFromKopecks } from '@/lib/format/money';
import { formatServiceRequestStatus } from '@/lib/format/labels';

export function CoworkingServiceRequestsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [items, setItems] = useState<ServiceRequestQueueItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setItems(await requestJson<ServiceRequestQueueItemDto[]>(`/api/coworkings/${coworkingId}/users/service-requests`));
  }

  useEffect(() => {
    let mounted = true;
    load()
      .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить сервисные заявки.'))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [coworkingId]);

  if (contextLoading || loading) return <FullPageLoader label="Загрузка сервисных заявок..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить сервисные заявки.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}><h2 className="mb-0">Сервисные
    заявки</h2><Card className="content-card"><Card.Body><Table responsive hover className="mt-3">
    <thead>
    <tr>
      <th>Пользователь</th>
      <th>Тип</th>
      <th>Название</th>
      <th>Стоимость</th>
      <th>Статус</th>
      <th>Действия</th>
    </tr>
    </thead>
    <tbody>{items.length === 0 ? <tr>
      <td colSpan={6} className="text-center text-body-secondary py-4">Сервисные заявки не найдены.</td>
    </tr> : items.map((item) => <tr key={item.serviceRequestId}>
      <td>{item.userName}</td>
      <td>{item.typeName}</td>
      <td>{item.name}</td>
      <td>{formatOptionalRublesFromKopecks(item.cost)}</td>
      <td>{formatServiceRequestStatus(item.status)}</td>
      <td><Button size="sm" variant="outline-primary"
                  href={`/coworkings/${coworkingId}/users/service-requests/${item.serviceRequestId}`}>Открыть</Button>
      </td>
    </tr>)}</tbody>
  </Table></Card.Body></Card></Stack></Container></main>;
}
