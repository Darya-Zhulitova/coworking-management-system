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
import type { ServiceRequestTypeDto } from '@/types/place';
import { formatRublesFromKopecks, kopecksToRublesInput, rublesInputToKopecks } from '@/lib/format/money';

interface EditState {
  name: string;
  cost: string;
  active: boolean;
}

export function CoworkingServiceRequestTypesPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [items, setItems] = useState<ServiceRequestTypeDto[]>([]);
  const [edits, setEdits] = useState<Record<number, EditState>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | 'create' | null>(null);
  const [form, setForm] = useState({ name: '', cost: '0.00' });
  const canManage = useMemo(() => context?.grants.includes('SERVICE_REQUEST_TYPE_EDIT') ?? false, [context]);

  const load = useCallback(async () => {
    const loadedItems = await requestJson<ServiceRequestTypeDto[]>(`/api/coworkings/${coworkingId}/service-request-types`);
    setItems(loadedItems);
    setEdits(Object.fromEntries(loadedItems.map((item) => [item.id, {
      name: item.name,
      cost: kopecksToRublesInput(item.cost),
      active: item.active
    }] as const)));
  }, [coworkingId]);

  useEffect(() => {
    let mounted = true;
    load().catch((error) => {
      if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить типы сервисных заявок.');
    }).finally(() => {
      if (mounted) setIsLoading(false);
    });
    return () => {
      mounted = false;
    };
  }, [load]);

  async function createItem(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingId('create');
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/service-request-types`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: form.name, cost: rublesInputToKopecks(form.cost) }),
      });
      setForm({ name: '', cost: '0.00' });
      await load();
      setSubmitMessage('Тип сервисной заявки создан.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось создать тип сервисной заявки.');
    } finally {
      setSavingId(null);
    }
  }

  async function saveItem(itemId: number) {
    const draft = edits[itemId];
    if (!draft) return;
    setSavingId(itemId);
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/service-request-types/${itemId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: draft.name, cost: rublesInputToKopecks(draft.cost), active: draft.active }),
      });
      await load();
      setSubmitMessage('Тип сервисной заявки обновлён.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить тип сервисной заявки.');
    } finally {
      setSavingId(null);
    }
  }

  async function archiveItem(itemId: number) {
    setSavingId(itemId);
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/service-request-types/${itemId}`, { method: 'DELETE' });
      await load();
      setSubmitMessage('Тип сервисной заявки отправлен в архив.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось архивировать тип сервисной заявки.');
    } finally {
      setSavingId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка типов сервисных заявок..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <div><h2 className="mb-2">Типы сервисных заявок</h2><p className="mb-0 text-body-secondary">Шаблоны заявок, которые
      копируются в пользовательские сервисные заявки при создании.</p></div>
    {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
    {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
    <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Создать тип
      заявки</Card.Title>{canManage ? <Form onSubmit={createItem}><Stack gap={3}><Form.Group
        controlId="serviceRequestTypeName"><Form.Label>Название</Form.Label><Form.Control value={form.name}
                                                                                          onChange={(event) => setForm((current) => ({
                                                                                            ...current,
                                                                                            name: event.target.value
                                                                                          }))}
                                                                                          required/></Form.Group><Form.Group
        controlId="serviceRequestTypeCost"><Form.Label>Стоимость, ₽</Form.Label><Form.Control type="number" min={0}
                                                                                              step="0.01"
                                                                                              value={form.cost}
                                                                                              onChange={(event) => setForm((current) => ({
                                                                                                ...current,
                                                                                                cost: event.target.value
                                                                                              }))}
                                                                                              required/></Form.Group><Button
        type="submit" disabled={savingId === 'create'}>Создать тип заявки</Button></Stack></Form> :
      <Alert variant="secondary" className="mb-0">Недостаточно прав для изменения.</Alert>}</Card.Body></Card>
    <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Типы сервисных заявок</Card.Title><Table
      responsive hover>
      <thead>
      <tr>
        <th>Название</th>
        <th>Стоимость</th>
        <th>Версия</th>
        <th>Статус</th>
        <th>Действия</th>
      </tr>
      </thead>
      <tbody>{items.map((item) => {
        const draft = edits[item.id] ?? { name: item.name, cost: kopecksToRublesInput(item.cost), active: item.active };
        return <tr key={item.id}>
          <td>{canManage ? <Form.Control value={draft.name} onChange={(event) => setEdits((current) => ({
            ...current,
            [item.id]: { ...draft, name: event.target.value }
          }))}/> : item.name}</td>
          <td>{canManage ? <Form.Control type="number" min={0} step="0.01" value={draft.cost}
                                         onChange={(event) => setEdits((current) => ({
                                           ...current,
                                           [item.id]: { ...draft, cost: event.target.value }
                                         }))}/> : formatRublesFromKopecks(item.cost)}</td>
          <td>{item.version}</td>
          <td><Stack direction="horizontal" gap={2}><Badge
            bg={item.active ? 'success' : 'secondary'}>{item.active ? 'Активен' : 'Неактивен'}</Badge>{canManage ?
            <Form.Check type="switch" id={`service-request-type-active-${item.id}`} checked={draft.active}
                        onChange={(event) => setEdits((current) => ({
                          ...current,
                          [item.id]: { ...draft, active: event.target.checked }
                        }))} label="Можно изменять"/> : null}</Stack></td>
          <td>{canManage ? <Stack direction="horizontal" gap={2}><Button size="sm" variant="outline-primary"
                                                                         disabled={savingId === item.id}
                                                                         onClick={() => saveItem(item.id)}>Сохранить</Button><Button
            size="sm" variant="outline-danger" disabled={savingId === item.id} onClick={() => archiveItem(item.id)}>В
            архив</Button></Stack> : '—'}</td>
        </tr>;
      })}</tbody>
    </Table></Card.Body></Card>
  </Stack></Container></main>;
}
