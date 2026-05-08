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
import type { PlaceTypeDto, TariffDto } from '@/types/place';

export function CoworkingPlaceTypesPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [tariffs, setTariffs] = useState<TariffDto[]>([]);
  const [placeTypes, setPlaceTypes] = useState<PlaceTypeDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | 'create' | null>(null);
  const [form, setForm] = useState({ name: '', tariffId: '' });
  const canManage = useMemo(() => context?.grants.includes('PLACE_TYPE_EDIT') ?? false, [context]);

  const load = useCallback(async () => {
    const [loadedTariffs, loadedPlaceTypes] = await Promise.all([
      requestJson<TariffDto[]>(`/api/coworkings/${coworkingId}/tariffs`),
      requestJson<PlaceTypeDto[]>(`/api/coworkings/${coworkingId}/place-types`),
    ]);
    setTariffs(loadedTariffs);
    setPlaceTypes(loadedPlaceTypes);
  }, [coworkingId]);
  useEffect(() => {
    let mounted = true;
    load().catch((error) => {
      if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить типы мест.');
    }).finally(() => {
      if (mounted) setIsLoading(false);
    });
    return () => {
      mounted = false;
    };
  }, [load]);
  useEffect(() => {
    if (tariffs[0] && !form.tariffId) setForm((c) => ({ ...c, tariffId: String(tariffs[0].id) }));
  }, [tariffs, form.tariffId]);

  async function createItem(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingId('create');
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/place-types`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: form.name, tariffId: Number(form.tariffId) })
      });
      setForm({ name: '', tariffId: tariffs[0] ? String(tariffs[0].id) : '' });
      await load();
      setSubmitMessage('Тип места создан.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось создать тип места.');
    } finally {
      setSavingId(null);
    }
  }

  async function toggleItem(item: PlaceTypeDto) {
    setSavingId(item.id);
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/place-types/${item.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: item.name, active: !item.active })
      });
      await load();
      setSubmitMessage(item.active ? 'Тип места деактивирован.' : 'Тип места активирован.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить тип места.');
    } finally {
      setSavingId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка типов мест..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <div><h2 className="mb-2">Типы мест</h2><p className="mb-0 text-body-secondary">Каждый тип места привязан к одному
      тарифу.</p></div>
    {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
    {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
    <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Создать тип
      места</Card.Title>{canManage ?
      <Form onSubmit={createItem}><Stack gap={3}><Form.Control placeholder="Название типа места" value={form.name}
                                                               onChange={(e) => setForm((c) => ({
                                                                 ...c,
                                                                 name: e.target.value
                                                               }))} required/><Form.Select value={form.tariffId}
                                                                                           onChange={(e) => setForm((c) => ({
                                                                                             ...c,
                                                                                             tariffId: e.target.value
                                                                                           }))}>{tariffs.map((tariff) =>
        <option key={tariff.id} value={tariff.id}>{tariff.name}</option>)}</Form.Select><Button type="submit"
                                                                                                disabled={savingId === 'create'}>Создать
        тип места</Button></Stack></Form> :
      <Alert variant="secondary" className="mb-0">Недостаточно прав для изменения.</Alert>}</Card.Body></Card>
    <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Типы мест</Card.Title><Table
      responsive hover>
      <thead>
      <tr>
        <th>Название</th>
        <th>Тариф</th>
        <th>Статус</th>
        <th>Действия</th>
      </tr>
      </thead>
      <tbody>{placeTypes.map((item) => <tr key={item.id}>
        <td>{item.name}</td>
        <td>{item.tariff.name}</td>
        <td><Badge bg={item.active ? 'success' : 'secondary'}>{item.active ? 'Активен' : 'Неактивен'}</Badge></td>
        <td>{canManage ? <Button size="sm" variant="outline-primary" disabled={savingId === item.id}
                                 onClick={() => toggleItem(item)}>{item.active ? 'Деактивировать' : 'Активировать'}</Button> : '—'}</td>
      </tr>)}</tbody>
    </Table></Card.Body></Card>
  </Stack></Container></main>;
}
