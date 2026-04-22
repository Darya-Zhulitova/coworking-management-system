'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
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
import type { FloorDto } from '@/types/place';

export function CoworkingFloorsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [floors, setFloors] = useState<FloorDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | 'create' | null>(null);
  const [form, setForm] = useState({ name: '', imageFileId: '' });
  const canManage = useMemo(() => context?.grants.includes('FLOOR_EDIT') ?? false, [context]);

  const load = useCallback(async () => setFloors(await requestJson<FloorDto[]>(`/api/coworkings/${coworkingId}/floors`)), [coworkingId]);
  useEffect(() => {
    let mounted = true;
    load().catch((error) => {
      if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить этажи.');
    }).finally(() => {
      if (mounted) setIsLoading(false);
    });
    return () => {
      mounted = false;
    };
  }, [load]);

  async function createFloor(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingId('create');
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/floors`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form)
      });
      setForm({ name: '', imageFileId: '' });
      await load();
      setSubmitMessage('Этаж создан.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось создать этаж.');
    } finally {
      setSavingId(null);
    }
  }

  async function toggleFloor(floor: FloorDto) {
    setSavingId(floor.id);
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/floors/${floor.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: floor.name, imageFileId: floor.imageFileId ?? '', active: !floor.active })
      });
      await load();
      setSubmitMessage(`Этаж ${floor.active ? 'деактивирован' : 'активирован'}.`);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить этаж.');
    } finally {
      setSavingId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка этажей..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return (
    <main className="page-shell"><Container className="py-4 py-md-5"><h2 className="mb-4">Редактирование этажей</h2>
      <Stack gap={4}>
        {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
        {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
        <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Создать этаж</Card.Title>
          {canManage ?
            <Form onSubmit={createFloor}><Stack gap={3}><Form.Control placeholder="Название этажа" value={form.name}
                                                                      onChange={(e) => setForm((c) => ({
                                                                        ...c,
                                                                        name: e.target.value
                                                                      }))} required/><Form.Control
              placeholder="ID изображения (необязательно)" value={form.imageFileId}
              onChange={(e) => setForm((c) => ({ ...c, imageFileId: e.target.value }))}/><Button type="submit"
                                                                                                 disabled={savingId === 'create'}>Создать
              этаж</Button></Stack></Form> :
            <Alert variant="secondary" className="mb-0">Недостаточно прав для изменения.</Alert>}
        </Card.Body></Card>
        <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Этажи</Card.Title>
          <Table responsive hover>
            <thead>
            <tr>
              <th>Название</th>
              <th>Карта</th>
              <th>Статус</th>
              <th>Действия</th>
            </tr>
            </thead>
            <tbody>
            {floors.map((floor) => <tr key={floor.id}>
              <td>{floor.name}</td>
              <td>{floor.imageFileId ?? '—'}</td>
              <td><Badge bg={floor.active ? 'success' : 'secondary'}>{floor.active ? 'Активен' : 'Неактивен'}</Badge>
              </td>
              <td><Stack direction="horizontal" gap={2}><Button size="sm"
                                                                href={`/coworkings/${coworkingId}/settings/floors/${floor.id}`}>Открыть
                места</Button>{canManage ? <Button size="sm" variant="outline-primary" disabled={savingId === floor.id}
                                                   onClick={() => toggleFloor(floor)}>{floor.active ? 'Деактивировать' : 'Активировать'}</Button> : null}
              </Stack></td>
            </tr>)}
            </tbody>
          </Table></Card.Body></Card>
      </Stack></Container></main>
  );
}
