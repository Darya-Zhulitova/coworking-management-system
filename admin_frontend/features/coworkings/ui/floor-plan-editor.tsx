'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { requestJson } from '@/lib/client/api';
import type { FloorDto, PlaceDto } from '@/types/place';

type Coordinate = { x: number; y: number };

type DragState = {
  placeId: number;
};

function clamp(value: number): number {
  return Math.min(1, Math.max(0, value));
}

function isPlaced(place: PlaceDto): boolean {
  return place.locX != null && place.locY != null;
}

function pointFromEvent(event: React.PointerEvent<HTMLElement>, element: HTMLElement): Coordinate {
  const rect = element.getBoundingClientRect();
  return {
    x: clamp((event.clientX - rect.left) / rect.width),
    y: clamp((event.clientY - rect.top) / rect.height),
  };
}

async function savePlaceCoordinates(coworkingId: number, place: PlaceDto, coordinates: Coordinate): Promise<PlaceDto> {
  return requestJson<PlaceDto>(`/api/coworkings/${coworkingId}/places/${place.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: place.name,
      locX: Number(coordinates.x.toFixed(4)),
      locY: Number(coordinates.y.toFixed(4)),
      amenities: place.amenities ?? [],
      active: place.active,
    }),
  });
}

async function clearPlaceCoordinates(coworkingId: number, place: PlaceDto): Promise<PlaceDto> {
  return requestJson<PlaceDto>(`/api/coworkings/${coworkingId}/places/${place.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: place.name,
      amenities: place.amenities ?? [],
      active: place.active,
    }),
  });
}

export function FloorPlanEditor({
                                  coworkingId,
                                  floor,
                                  places,
                                  canManage,
                                  onReload,
                                }: {
  coworkingId: number;
  floor: FloorDto;
  places: PlaceDto[];
  canManage: boolean;
  onReload: () => Promise<void>;
}) {
  const mapRef = useRef<HTMLDivElement | null>(null);
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const [localPlaces, setLocalPlaces] = useState<PlaceDto[]>(places);
  const [dragState, setDragState] = useState<DragState | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [savingPlaceId, setSavingPlaceId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setLocalPlaces(places);
  }, [places]);

  const selectedPlace = useMemo(
    () => localPlaces.find((place) => place.id === selectedPlaceId) ?? null,
    [localPlaces, selectedPlaceId],
  );
  const placedPlaces = localPlaces.filter(isPlaced);
  const unplacedPlaces = localPlaces.filter((place) => !isPlaced(place));
  const hasPlan = Boolean(floor.imageUrl || floor.imageFileId);

  async function uploadPlan(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const input = event.currentTarget.elements.namedItem('file');
    if (!(input instanceof HTMLInputElement) || !input.files?.[0]) {
      setErrorMessage('Выберите изображение плана этажа.');
      return;
    }
    const formData = new FormData();
    formData.set('file', input.files[0]);
    setIsUploading(true);
    setErrorMessage(null);
    setMessage(null);
    try {
      await requestJson<FloorDto>(`/api/coworkings/${coworkingId}/floors/${floor.id}/plan`, {
        method: 'POST',
        body: formData,
      });
      input.value = '';
      await onReload();
      setMessage('План этажа загружен. Теперь можно расставить места.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить план этажа.');
    } finally {
      setIsUploading(false);
    }
  }

  async function placeSelectedAt(event: React.PointerEvent<HTMLDivElement>) {
    if (!canManage || dragState || !selectedPlace || !mapRef.current) return;
    const coordinates = pointFromEvent(event, mapRef.current);
    await saveCoordinates(selectedPlace, coordinates, 'Место размещено на плане.');
  }

  function startDrag(event: React.PointerEvent<HTMLButtonElement>, place: PlaceDto) {
    if (!canManage) return;
    event.stopPropagation();
    event.currentTarget.setPointerCapture(event.pointerId);
    setSelectedPlaceId(place.id);
    setDragState({ placeId: place.id });
  }

  function moveDrag(event: React.PointerEvent<HTMLButtonElement>) {
    if (!dragState || !mapRef.current) return;
    event.stopPropagation();
    const coordinates = pointFromEvent(event, mapRef.current);
    setLocalPlaces((current) => current.map((place) => place.id === dragState.placeId
      ? { ...place, locX: coordinates.x, locY: coordinates.y }
      : place));
  }

  async function finishDrag(event: React.PointerEvent<HTMLButtonElement>, place: PlaceDto) {
    if (!dragState || !mapRef.current) return;
    event.stopPropagation();
    const coordinates = pointFromEvent(event, mapRef.current);
    setDragState(null);
    await saveCoordinates(place, coordinates, 'Позиция места сохранена.');
  }

  async function saveCoordinates(place: PlaceDto, coordinates: Coordinate, successMessage: string) {
    setSavingPlaceId(place.id);
    setErrorMessage(null);
    setMessage(null);
    try {
      const updatedPlace = await savePlaceCoordinates(coworkingId, place, coordinates);
      setLocalPlaces((current) => current.map((item) => item.id === place.id ? updatedPlace : item));
      setMessage(successMessage);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось сохранить координаты места.');
      await onReload();
    } finally {
      setSavingPlaceId(null);
    }
  }

  async function removeFromPlan(place: PlaceDto) {
    setSavingPlaceId(place.id);
    setErrorMessage(null);
    setMessage(null);
    try {
      const updatedPlace = await clearPlaceCoordinates(coworkingId, place);
      setLocalPlaces((current) => current.map((item) => item.id === place.id ? updatedPlace : item));
      setSelectedPlaceId(null);
      setMessage('Место убрано с плана.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось убрать место с плана.');
    } finally {
      setSavingPlaceId(null);
    }
  }

  return <Card className="content-card">
    <Card.Body>
      <Stack direction="horizontal" className="justify-content-between align-items-start flex-wrap gap-3 mb-3">
        <div>
          <Card.Title as="h2" className="h4 mb-1">Карта этажа</Card.Title>
          <p className="text-body-secondary mb-0">Загрузите план и разместите места поверх изображения.</p>
        </div>
        {hasPlan ? <Badge bg="success">Карта включена</Badge> : <Badge bg="secondary">Карта не загружена</Badge>}
      </Stack>

      {message ? <Alert variant="success">{message}</Alert> : null}
      {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}

      {canManage ? <Form onSubmit={uploadPlan} className="mb-4">
        <Row className="g-2 align-items-end">
          <Col md={8}>
            <Form.Label>Изображение плана этажа</Form.Label>
            <Form.Control name="file" type="file" accept="image/png,image/jpeg,image/webp"/>
          </Col>
          <Col md={4}>
            <Button type="submit" disabled={isUploading} className="w-100">
              {hasPlan ? 'Заменить план' : 'Загрузить план'}
            </Button>
          </Col>
        </Row>
      </Form> : null}

      {!hasPlan ? <Alert variant="secondary" className="mb-0">
        Для этажа пока нет плана. После загрузки изображения карта станет доступна для расстановки мест.
      </Alert> : <Row className="g-4">
        <Col lg={8}>
          <div
            ref={mapRef}
            role="presentation"
            className="position-relative border rounded overflow-hidden bg-light floor-plan-map"
            onPointerDown={placeSelectedAt}
          >
            <img src={floor.imageUrl ?? ''} alt={`План этажа ${floor.name}`} className="d-block w-100"/>
            {placedPlaces.map((place) => <button
              key={place.id}
              type="button"
              className={`floor-plan-marker btn btn-sm ${selectedPlaceId === place.id ? 'btn-primary' : 'btn-light'}`}
              style={{ left: `${(place.locX ?? 0) * 100}%`, top: `${(place.locY ?? 0) * 100}%` }}
              disabled={savingPlaceId === place.id}
              onPointerDown={(event) => startDrag(event, place)}
              onPointerMove={moveDrag}
              onPointerUp={(event) => finishDrag(event, place)}
              onClick={(event) => {
                event.stopPropagation();
                setSelectedPlaceId(place.id);
              }}
              title={place.name}
            >{place.name}</button>)}
          </div>
          <div className="text-body-secondary small mt-2">
            Выберите неразмещённое место и кликните по плану. Уже размещённые места можно перетаскивать мышью.
          </div>
        </Col>
        <Col lg={4}>
          <Stack gap={3}>
            <div>
              <h3 className="h6">Неразмещённые места</h3>
              {unplacedPlaces.length === 0 ? <p className="text-body-secondary small mb-0">Все места размещены.</p> :
                <Stack gap={2}>{unplacedPlaces.map((place) => <Button
                  key={place.id}
                  type="button"
                  size="sm"
                  variant={selectedPlaceId === place.id ? 'primary' : 'outline-primary'}
                  disabled={!canManage || savingPlaceId === place.id}
                  onClick={() => setSelectedPlaceId(place.id)}
                >{place.name}</Button>)}</Stack>}
            </div>
            <div>
              <h3 className="h6">Размещённые места</h3>
              {placedPlaces.length === 0 ? <p className="text-body-secondary small mb-0">На плане пока нет мест.</p> :
                <Table responsive size="sm" className="align-middle mb-0">
                  <tbody>{placedPlaces.map((place) => <tr key={place.id}>
                    <td><Button size="sm" variant="link" className="p-0"
                                onClick={() => setSelectedPlaceId(place.id)}>{place.name}</Button></td>
                    <td className="text-end">{canManage ?
                      <Button size="sm" variant="outline-danger" disabled={savingPlaceId === place.id}
                              onClick={() => removeFromPlan(place)}>Убрать</Button> : null}</td>
                  </tr>)}</tbody>
                </Table>}
            </div>
          </Stack>
        </Col>
      </Row>}
    </Card.Body>
  </Card>;
}
