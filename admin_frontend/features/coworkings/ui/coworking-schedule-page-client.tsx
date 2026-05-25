'use client';

import { useCallback, useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Modal from 'react-bootstrap/Modal';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import { OperationalImpactPreview } from '@/features/coworkings/ui/operational-impact-preview';
import type {
  CoworkingScheduleDto,
  CoworkingScheduleExceptionDto,
  OperationalImpactDto,
  PlaceClosingDto,
  PlaceDto
} from '@/types/place';

const WEEKDAY_FIELDS: Array<{ key: keyof Omit<CoworkingScheduleDto, 'schedule'>; label: string }> = [
  { key: 'monday', label: 'Понедельник' }, { key: 'tuesday', label: 'Вторник' }, { key: 'wednesday', label: 'Среда' },
  { key: 'thursday', label: 'Четверг' }, { key: 'friday', label: 'Пятница' }, {
    key: 'saturday',
    label: 'Суббота'
  }, { key: 'sunday', label: 'Воскресенье' },
];
const EMPTY_SCHEDULE: CoworkingScheduleDto = {
  schedule: 0,
  monday: false,
  tuesday: false,
  wednesday: false,
  thursday: false,
  friday: false,
  saturday: false,
  sunday: false
};

type PendingOperation = {
  title: string;
  commitUrl: string;
  payload: Record<string, unknown>;
  impactHash: string;
  afterCommit: () => void | Promise<void>;
};

export function CoworkingSchedulePageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [schedule, setSchedule] = useState<CoworkingScheduleDto>(EMPTY_SCHEDULE);
  const [exceptions, setExceptions] = useState<CoworkingScheduleExceptionDto[]>([]);
  const [closings, setClosings] = useState<PlaceClosingDto[]>([]);
  const [places, setPlaces] = useState<PlaceDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [impact, setImpact] = useState<OperationalImpactDto | null>(null);
  const [pendingOperation, setPendingOperation] = useState<PendingOperation | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [exceptionForm, setExceptionForm] = useState({ date: '', type: 'CLOSE', name: '' });
  const [closingForm, setClosingForm] = useState({ placeId: '', date: '', name: '' });
  const canEdit = context?.grants.includes('SCHEDULE_EDIT') ?? false;

  const load = useCallback(async () => {
    const [scheduleData, exceptionData, closingData, placeData] = await Promise.all([
      requestJson<CoworkingScheduleDto>(`/api/coworkings/${coworkingId}/schedule`),
      requestJson<CoworkingScheduleExceptionDto[]>(`/api/coworkings/${coworkingId}/schedule/exceptions`),
      requestJson<PlaceClosingDto[]>(`/api/coworkings/${coworkingId}/schedule/closings`),
      requestJson<PlaceDto[]>(`/api/coworkings/${coworkingId}/places`),
    ]);
    setSchedule(scheduleData);
    setExceptions(exceptionData);
    setClosings(closingData);
    setPlaces(placeData);
    if (placeData[0] && !closingForm.placeId) setClosingForm((current) => ({
      ...current,
      placeId: String(placeData[0].id)
    }));
  }, [coworkingId, closingForm.placeId]);

  useEffect(() => {
    let mounted = true;
    load().catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить данные расписания.')).finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [load]);

  function schedulePayload() {
    return {
      monday: schedule.monday,
      tuesday: schedule.tuesday,
      wednesday: schedule.wednesday,
      thursday: schedule.thursday,
      friday: schedule.friday,
      saturday: schedule.saturday,
      sunday: schedule.sunday
    };
  }

  async function prepareImpact(title: string, previewUrl: string, commitUrl: string, payload: Record<string, unknown>, afterCommit: PendingOperation['afterCommit']) {
    setMessage(null);
    const response = await requestJson<OperationalImpactDto>(previewUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    setImpact(response);
    setPendingOperation({ title, commitUrl, payload, impactHash: response.impactHash, afterCommit });
  }

  async function updateSchedule(event: React.FormEvent) {
    event.preventDefault();
    const payload = schedulePayload();
    await prepareImpact('Подтвердить изменение расписания', `/api/coworkings/${coworkingId}/schedule/preview`, `/api/coworkings/${coworkingId}/schedule/commit`, payload, async () => {
      await load();
      setMessage('Расписание обновлено.');
    });
  }

  async function createException(event: React.FormEvent) {
    event.preventDefault();

    if (exceptionForm.type === 'CLOSE') {
      await prepareImpact('Подтвердить добавление закрытия', `/api/coworkings/${coworkingId}/schedule/exceptions/preview`, `/api/coworkings/${coworkingId}/schedule/exceptions/commit`, exceptionForm, async () => {
        setExceptionForm({ date: '', type: 'CLOSE', name: '' });
        await load();
        setMessage('Исключение в расписании создано.');
      });
      return;
    }

    await requestJson(`/api/coworkings/${coworkingId}/schedule/exceptions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(exceptionForm)
    });
    setExceptionForm({ date: '', type: 'CLOSE', name: '' });
    await load();
    setMessage('Исключение в расписании создано.');
  }

  async function createClosing(event: React.FormEvent) {
    event.preventDefault();
    const payload = { ...closingForm, placeId: Number(closingForm.placeId) };
    await prepareImpact('Подтвердить закрытие места', `/api/coworkings/${coworkingId}/schedule/closings/preview`, `/api/coworkings/${coworkingId}/schedule/closings/commit`, payload, async () => {
      setClosingForm({ placeId: places[0] ? String(places[0].id) : '', date: '', name: '' });
      await load();
      setMessage('Закрытие места создано.');
    });
  }

  async function confirmPendingOperation() {
    if (!pendingOperation) return;
    setConfirming(true);
    setError(null);
    try {
      const response = await requestJson<OperationalImpactDto>(pendingOperation.commitUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...pendingOperation.payload, impactHash: pendingOperation.impactHash })
      });
      setImpact(response);
      await pendingOperation.afterCommit();
      setPendingOperation(null);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Не удалось подтвердить операцию.');
    } finally {
      setConfirming(false);
    }
  }

  async function archiveException(id: number) {
    await requestJson(`/api/coworkings/${coworkingId}/schedule/exceptions/${id}`, { method: 'DELETE' });
    await load();
  }

  async function archiveClosing(id: number) {
    await requestJson(`/api/coworkings/${coworkingId}/schedule/closings/${id}`, { method: 'DELETE' });
    await load();
  }

  if (contextLoading || loading) return <FullPageLoader label="Загрузка расписания..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить расписание.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <div><h2 className="mb-2">Расписание</h2><p className="mb-0 text-body-secondary">Управление недельным расписанием,
      исключениями и закрытиями отдельных мест.</p></div>
    {message ? <Alert variant="success">{message}</Alert> : null}
    {/*{impact ?*/}
    {/*  <Alert variant="warning">{} Затронутые бронирования: {impact.affectedBookingsCount}.*/}
    {/*    Даты: {impact.affectedDates.join(', ') || 'нет'}.*/}
    {/*    Команды: {formatUserDomainCommandList(impact.plannedUserDomainCommands) || 'нет'}.</Alert> : null}*/}
    <Row className="g-4"><Col lg={4}><Card className="content-card h-100"><Card.Body><Card.Title as="h2"
                                                                                                 className="h4 mb-3">Недельное
      расписание</Card.Title>{canEdit ?
      <Form onSubmit={updateSchedule}><Stack gap={2}>{WEEKDAY_FIELDS.map((field) => <Form.Check key={field.key}
                                                                                                type="switch"
                                                                                                id={`weekday-${field.key}`}
                                                                                                label={field.label}
                                                                                                checked={schedule[field.key]}
                                                                                                onChange={(event) => setSchedule((current) => ({
                                                                                                  ...current,
                                                                                                  [field.key]: event.target.checked
                                                                                                }))}/>)}<Button
        type="submit">Сохранить расписание</Button></Stack></Form> :
      <Alert variant="secondary">Недостаточно прав для изменения.</Alert>}</Card.Body></Card></Col><Col lg={8}><Card
      className="content-card h-100"><Card.Body><Card.Title as="h2" className="h4 mb-3">Исключения в
      расписании</Card.Title>{canEdit ?
      <Form onSubmit={createException} className="mb-4"><Stack gap={2}><Form.Control type="date"
                                                                                     value={exceptionForm.date}
                                                                                     onChange={(e) => setExceptionForm((c) => ({
                                                                                       ...c,
                                                                                       date: e.target.value
                                                                                     }))} required/><Form.Select
        value={exceptionForm.type} onChange={(e) => setExceptionForm((c) => ({ ...c, type: e.target.value }))}>
        <option value="OPEN">Открыть</option>
        <option value="CLOSE">Закрыть</option>
      </Form.Select><Form.Control value={exceptionForm.name}
                                  onChange={(e) => setExceptionForm((c) => ({ ...c, name: e.target.value }))}
                                  placeholder="Причина" required/><Button type="submit">Добавить
        исключение</Button></Stack></Form> : null}<Table responsive hover>
      <thead>
      <tr>
        <th>Дата</th>
        <th>Тип</th>
        <th>Название</th>
        <th>Действия</th>
      </tr>
      </thead>
      <tbody>{exceptions.map((item) => <tr key={item.id}>
        <td>{item.date}</td>
        <td>{item.type === 'OPEN' ? 'Открыть' : 'Закрыть'}</td>
        <td>{item.name}</td>
        <td>{canEdit ? <Button size="sm" variant="outline-danger" onClick={() => archiveException(item.id)}>В
          архив</Button> : '—'}</td>
      </tr>)}</tbody>
    </Table></Card.Body></Card></Col></Row>
    <Row className="g-4"><Col><Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Закрытия
      мест</Card.Title>{canEdit ?
      <Form onSubmit={createClosing} className="mb-4"><Stack direction="horizontal" gap={2}
                                                             className="flex-wrap"><Form.Select
        value={closingForm.placeId}
        onChange={(e) => setClosingForm((c) => ({ ...c, placeId: e.target.value }))}>{places.map((place) => <option
        key={place.id} value={place.id}>{place.name}</option>)}</Form.Select><Form.Control type="date"
                                                                                           value={closingForm.date}
                                                                                           onChange={(e) => setClosingForm((c) => ({
                                                                                             ...c,
                                                                                             date: e.target.value
                                                                                           }))} required/><Form.Control
        value={closingForm.name} onChange={(e) => setClosingForm((c) => ({ ...c, name: e.target.value }))}
        placeholder="Причина" required/><Button type="submit">Добавить закрытие
        места</Button></Stack></Form> : null}<Table responsive hover>
      <thead>
      <tr>
        <th>Место</th>
        <th>Дата</th>
        <th>Название</th>
        <th>Действия</th>
      </tr>
      </thead>
      <tbody>{closings.map((item) => <tr key={item.id}>
        <td>{item.placeName}</td>
        <td>{item.date}</td>
        <td>{item.name}</td>
        <td>{canEdit ? <Button size="sm" variant="outline-danger" onClick={() => archiveClosing(item.id)}>В
          архив</Button> : '—'}</td>
      </tr>)}</tbody>
    </Table></Card.Body></Card></Col></Row>
    <Modal show={pendingOperation != null} onHide={() => !confirming && setPendingOperation(null)} size="xl"
           centered><Modal.Header
      closeButton><Modal.Title>{pendingOperation?.title}</Modal.Title></Modal.Header><Modal.Body>{impact ?
      <OperationalImpactPreview
        impact={impact}
        description="После подтверждения система отменит бронирования, которые больше не соответствуют новым правилам расписания, и автоматически начислит пользователям компенсации."
      /> : null}</Modal.Body><Modal.Footer><Button variant="outline-secondary"
                                                   onClick={() => setPendingOperation(null)}
                                                   disabled={confirming}>Отмена</Button><Button
      variant="primary" onClick={confirmPendingOperation}
      disabled={confirming}>{confirming ? 'Выполняется...' : 'Подтвердить'}</Button></Modal.Footer></Modal>
  </Stack></Container></main>;
}
