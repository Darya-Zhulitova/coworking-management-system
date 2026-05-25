'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Modal from 'react-bootstrap/Modal';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import { FloorPlanEditor } from '@/features/coworkings/ui/floor-plan-editor';
import { OperationalImpactPreview } from '@/features/coworkings/ui/operational-impact-preview';
import type {
  FloorDto,
  PlaceClosingDto,
  PlaceDeactivationPreview,
  PlaceDto,
  PlaceOperationalDto,
  PlaceTypeDto
} from '@/types/place';

export function CoworkingFloorDetailPageClient({ coworkingId, floorId }: { coworkingId: number; floorId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [floor, setFloor] = useState<FloorDto | null>(null);
  const [floorMapEnabled, setFloorMapEnabled] = useState(false);
  const [places, setPlaces] = useState<PlaceDto[]>([]);
  const [operational, setOperational] = useState<PlaceOperationalDto[]>([]);
  const [closings, setClosings] = useState<PlaceClosingDto[]>([]);
  const [placeTypes, setPlaceTypes] = useState<PlaceTypeDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [preview, setPreview] = useState<PlaceDeactivationPreview | null>(null);
  const [pendingDeactivationPlace, setPendingDeactivationPlace] = useState<PlaceDto | null>(null);
  const [savingId, setSavingId] = useState<number | 'create' | null>(null);
  const [form, setForm] = useState({ name: '', placeTypeId: '', amenities: '' });
  const [previewImage, setPreviewImage] = useState<{ url: string; title: string } | null>(null);
  const canManage = useMemo(() => context?.grants.includes('PLACE_EDIT') ?? false, [context]);

  const load = useCallback(async () => {
    const [coworking, allFloors, allPlaces, allPlaceTypes, operationalItems, placeClosings] = await Promise.all([
      requestJson<{ floorMapEnabled?: boolean }>(`/api/coworkings/${coworkingId}`),
      requestJson<FloorDto[]>(`/api/coworkings/${coworkingId}/floors`),
      requestJson<PlaceDto[]>(`/api/coworkings/${coworkingId}/places`),
      requestJson<PlaceTypeDto[]>(`/api/coworkings/${coworkingId}/place-types`),
      requestJson<PlaceOperationalDto[]>(`/api/coworkings/${coworkingId}/places/operational?floorId=${floorId}`),
      requestJson<PlaceClosingDto[]>(`/api/coworkings/${coworkingId}/schedule/closings?floorId=${floorId}`),
    ]);
    setFloorMapEnabled(Boolean(coworking.floorMapEnabled));
    setFloor(allFloors.find((item) => item.id === floorId) ?? null);
    setPlaces(allPlaces.filter((item) => item.floorId === floorId));
    setPlaceTypes(allPlaceTypes.filter((item) => item.active));
    setOperational(operationalItems);
    setClosings(placeClosings);
  }, [coworkingId, floorId]);

  useEffect(() => {
    let mounted = true;
    load().catch((error) => {
      if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить данные этажа.');
    }).finally(() => {
      if (mounted) setIsLoading(false);
    });
    return () => {
      mounted = false;
    };
  }, [load]);
  useEffect(() => {
    if (placeTypes[0] && !form.placeTypeId) setForm((c) => ({ ...c, placeTypeId: String(placeTypes[0].id) }));
  }, [placeTypes, form.placeTypeId]);

  async function createPlace(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingId('create');
    setErrorMessage(null);
    setPreview(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/places`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: form.name,
          floorId,
          placeTypeId: Number(form.placeTypeId),
          amenities: form.amenities.split(',').map((item) => item.trim()).filter(Boolean)
        })
      });
      setForm({
        name: '',
        placeTypeId: placeTypes[0] ? String(placeTypes[0].id) : '',
        amenities: ''
      });
      await load();
      setSubmitMessage('Место создано.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось создать место.');
    } finally {
      setSavingId(null);
    }
  }


  async function uploadPlacePhoto(place: PlaceDto, file: File) {
    setSavingId(place.id);
    setErrorMessage(null);
    setPreview(null);
    const formData = new FormData();
    formData.set('file', file);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/places/${place.id}/photo`, {
        method: 'POST',
        body: formData
      });
      await load();
      setSubmitMessage('Фото места загружено.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить фото места.');
    } finally {
      setSavingId(null);
    }
  }

  async function togglePlace(place: PlaceDto) {
    setSavingId(place.id);
    setErrorMessage(null);
    setPreview(null);
    try {
      if (place.active) {
        const response = await requestJson<PlaceDeactivationPreview>(`/api/coworkings/${coworkingId}/places/${place.id}/deactivation/preview`, { method: 'POST' });
        setPreview(response);
        setPendingDeactivationPlace(place);
      } else {
        await requestJson(`/api/coworkings/${coworkingId}/places/${place.id}/activate`, { method: 'POST' });
        setSubmitMessage('Место активировано.');
      }
      await load();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить место.');
    } finally {
      setSavingId(null);
    }
  }

  async function confirmPlaceDeactivation() {
    if (!pendingDeactivationPlace || !preview?.impactHash) {
      setErrorMessage('Сначала выполните предпросмотр изменений, затем подтвердите действие.');
      return;
    }
    setSavingId(pendingDeactivationPlace.id);
    setErrorMessage(null);
    try {
      const response = await requestJson<PlaceDeactivationPreview>(`/api/coworkings/${coworkingId}/places/${pendingDeactivationPlace.id}/deactivation/commit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ impactHash: preview.impactHash })
      });
      setPreview(response);
      setPendingDeactivationPlace(null);
      await load();
      setSubmitMessage('Место деактивировано.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось деактивировать место.');
    } finally {
      setSavingId(null);
    }
  }

  const getOperational = (placeId: number) => operational.find((item) => item.placeId === placeId);
  const getClosingsCount = (placeId: number) => closings.filter((item) => item.placeId === placeId).length;

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка данных по этажу..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  if (!floor) return <FullPageError message="Этаж не найден."/>;

  return <>
    <main className="page-shell"><Container className="py-4 py-md-5"><h2 className="mb-4">Редактирование этажа</h2>
      <Stack gap={4}>
        {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
        {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
        {/*{preview ? <Alert variant="warning" className="mb-0">{preview.summary} Затронутые*/}
        {/*  бронирования: {preview.simulatedAffectedFutureBookings}. Даты: {preview.affectedDates.join(', ') || 'нет'}.*/}
        {/*  Команды: {formatUserDomainCommandList(preview.plannedUserDomainCommands) || 'нет'}.</Alert> : null}*/}
        {floorMapEnabled ? (
          <FloorPlanEditor coworkingId={coworkingId} floor={floor} places={places} canManage={canManage}
                           onReload={load}/>
        ) : (
          <Alert variant="secondary" className="mb-0">
            Карты этажей выключены в настройках коворкинга. Настройки плана этажа и координат мест скрыты.
          </Alert>
        )}
        <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Добавить место на этот
          этаж</Card.Title>{canManage ?
          <Form onSubmit={createPlace}><Stack gap={3}><Form.Control placeholder="Название места" value={form.name}
                                                                    onChange={(e) => setForm((c) => ({
                                                                      ...c,
                                                                      name: e.target.value
                                                                    }))} required/><Form.Select value={form.placeTypeId}
                                                                                                onChange={(e) => setForm((c) => ({
                                                                                                  ...c,
                                                                                                  placeTypeId: e.target.value
                                                                                                }))}>{placeTypes.map((pt) =>
            <option key={pt.id} value={pt.id}>{pt.name} — {pt.tariff.name}</option>)}</Form.Select><Form.Control
            placeholder="Удобства (через запятую)" value={form.amenities}
            onChange={(e) => setForm((c) => ({ ...c, amenities: e.target.value }))}/><Button type="submit"
                                                                                             disabled={savingId === 'create'}>Создать
            место</Button></Stack></Form> :
          <Alert variant="secondary" className="mb-0">Недостаточно прав для изменения.</Alert>}</Card.Body></Card>
        <Card className="content-card"><Card.Body><Card.Title as="h2" className="h4 mb-3">Места на этом
          этаже</Card.Title><Table responsive hover>
          <thead>
          <tr>
            <th>Фото</th>
            <th>Название</th>
            <th>Тип</th>
            <th>Удобства</th>
            <th>Бронирования</th>
            <th>Активные бронирования</th>
            <th>Закрытия</th>
            <th>Статус</th>
            <th>Действия</th>
          </tr>
          </thead>
          <tbody>{places.map((place) => {
            const op = getOperational(place.id);
            return <tr key={place.id}>
              <td>{place.previewImageUrl ? <button type="button" className="border-0 bg-transparent p-0"
                                                   onClick={() => setPreviewImage({
                                                     url: place.fullImageUrl ?? place.imageUrl!,
                                                     title: place.name
                                                   })}><img src={place.previewImageUrl ?? place.imageUrl!}
                                                            alt={place.name} style={{
                width: 144,
                height: 108,
                objectFit: 'cover',
                borderRadius: 12,
                cursor: 'zoom-in'
              }}/></button> : <span className="text-body-secondary">—</span>}</td>
              <td>{place.name}</td>
              <td>{place.placeType.name}</td>
              <td>{place.amenities?.join(', ') || '—'}</td>
              <td>{op?.totalBookings ?? 0}</td>
              <td>{op?.unfinishedBookings ?? 0}</td>
              <td>{getClosingsCount(place.id)}</td>
              <td><Badge bg={place.active ? 'success' : 'secondary'}>{place.active ? 'Активен' : 'Неактивен'}</Badge>
              </td>
              <td><Stack direction="horizontal" gap={2} className="flex-wrap">{canManage ?
                <Button size="sm" variant="outline-primary" disabled={savingId === place.id}
                        onClick={() => togglePlace(place)}>{place.active ? 'Деактивировать' : 'Активировать'}</Button> : null}<Button
                size="sm" variant="outline-secondary" href={`/coworkings/${coworkingId}/settings/schedule`}>Расписание
                места</Button>{canManage ? <label className="btn btn-sm btn-outline-success mb-0">Фото
                <input type="file" accept="image/jpeg,image/png,image/webp"
                       title="JPG, PNG или WEBP, до 10 МБ. Preview будет сохранен в формате 4:3." hidden
                       disabled={savingId === place.id} onChange={(event) => {
                  const file = event.target.files?.[0];
                  event.currentTarget.value = '';
                  if (file) void uploadPlacePhoto(place, file);
                }}/>
              </label> : null}<Button size="sm" variant="outline-dark"
                                      href={`/coworkings/${coworkingId}/settings/floors/${floorId}/places/${place.id}/bookings`}>Управление
                бронированиями</Button></Stack>
              </td>
            </tr>;
          })}</tbody>
        </Table></Card.Body></Card>
      </Stack></Container></main>
    {previewImage ? <Modal show onHide={() => setPreviewImage(null)} centered size="lg"><Modal.Header
      closeButton><Modal.Title>{previewImage.title}</Modal.Title></Modal.Header><Modal.Body>{/* eslint-disable-next-line @next/next/no-img-element */}<img
      src={previewImage.url} alt={previewImage.title} className="w-100 rounded-3"
      style={{ maxHeight: '76vh', objectFit: 'contain' }}/></Modal.Body></Modal> : null}
    <Modal show={Boolean(pendingDeactivationPlace && preview)} onHide={() => setPendingDeactivationPlace(null)}
           size="lg" centered><Modal.Header closeButton><Modal.Title>Подтверждение деактивации
      места</Modal.Title></Modal.Header><Modal.Body>{preview ? <OperationalImpactPreview
      impact={preview}
      description="После подтверждения место станет недоступным. Все затронутые активные бронирования будут отменены, компенсации начислятся автоматически."
    /> : null}</Modal.Body><Modal.Footer><Button variant="outline-secondary"
                                                 onClick={() => setPendingDeactivationPlace(null)}>Отмена</Button><Button
      variant="danger" disabled={savingId === pendingDeactivationPlace?.id}
      onClick={confirmPlaceDeactivation}>Подтвердить</Button></Modal.Footer></Modal></>;
}
