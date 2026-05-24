'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Modal from 'react-bootstrap/Modal';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import { formatRublesFromKopecks } from '@/lib/format/money';
import { OperationalImpactPreview } from '@/features/coworkings/ui/operational-impact-preview';
import type { OperationalImpactDto, PlaceBookingListResponseDto, PlaceDto } from '@/types/place';

function formatBookingDate(value: string): string {
  if (!value) return '—';
  return value.slice(0, 10);
}

function formatBookingStatus(booking: { status: string; active: boolean }): string {
  if (!booking.active) return 'Неактивна';
  const labels: Record<string, string> = {
    ACTUAL: 'Активна',
    active: 'Активна',
    ACTIVE: 'Активна',
    canceled: 'Отменена',
    CANCELED: 'Отменена',
    CANCELED_USER: 'Отменена пользователем',
    CANCELED_ADMIN: 'Отменена администратором',
  };
  return labels[booking.status] ?? booking.status;
}

type PendingClosing = {
  bookingId: number;
  date: string;
  payload: { placeId: number; date: string; name: string };
  impactHash: string;
};

export function PlaceBookingsPageClient({ coworkingId, floorId, placeId }: {
  coworkingId: number;
  floorId: number;
  placeId: number;
}) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true,
  });
  const [place, setPlace] = useState<PlaceDto | null>(null);
  const [bookingData, setBookingData] = useState<PlaceBookingListResponseDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [impact, setImpact] = useState<OperationalImpactDto | null>(null);
  const [pendingClosing, setPendingClosing] = useState<PendingClosing | null>(null);
  const [confirming, setConfirming] = useState(false);

  const bookings = useMemo(() => bookingData?.bookings ?? [], [bookingData]);

  const load = useCallback(async () => {
    const [places, response] = await Promise.all([
      requestJson<PlaceDto[]>(`/api/coworkings/${coworkingId}/places`),
      requestJson<PlaceBookingListResponseDto>(`/api/coworkings/${coworkingId}/places/${placeId}/bookings`),
    ]);
    setPlace(places.find((item) => item.id === placeId) ?? null);
    setBookingData(response);
  }, [coworkingId, placeId]);

  useEffect(() => {
    let mounted = true;
    load()
      .catch((error) => {
        if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить бронирования места.');
      })
      .finally(() => {
        if (mounted) setIsLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [load]);

  async function openCancelModal(booking: { bookingId: number; date: string }) {
    setActionError(null);
    setActionMessage(null);
    const payload = {
      placeId,
      date: formatBookingDate(booking.date),
      name: `Отмена бронирования ${booking.bookingNumber ?? booking.bookingId}`,
    };
    try {
      const preview = await requestJson<OperationalImpactDto>(`/api/coworkings/${coworkingId}/schedule/closings/preview`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      setImpact(preview);
      setPendingClosing({ bookingId: booking.bookingId, date: payload.date, payload, impactHash: preview.impactHash });
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Не удалось рассчитать влияние закрытия места.');
    }
  }

  async function confirmCancelThroughClosing() {
    if (!pendingClosing) return;
    setConfirming(true);
    setActionError(null);
    setActionMessage(null);
    try {
      const committed = await requestJson<OperationalImpactDto>(`/api/coworkings/${coworkingId}/schedule/closings/commit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...pendingClosing.payload, impactHash: pendingClosing.impactHash }),
      });
      setImpact(committed);
      await load();
      setPendingClosing(null);
      setActionMessage(`Создано исключение места. Отменено бронирований: ${committed.affectedBookingsCount}.`);
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Не удалось отменить бронь через исключение места.');
    } finally {
      setConfirming(false);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка бронирований места..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  if (errorMessage) return <FullPageError message={errorMessage}/>;
  if (!place) return <FullPageError message="Место не найдено."/>;

  return <main className="page-shell">
    <Container className="py-4 py-md-5">
      <Stack gap={4}>
        <Card className="content-card">
          <Card.Body>
            <Stack direction="horizontal" gap={3} className="flex-wrap justify-content-between align-items-start">
              <div>
                <p className="text-uppercase text-body-secondary small mb-1">Место</p>
                <h1 className="h3 mb-2">Управление бронированиями: {place.name}</h1>
                <p className="mb-0 text-body-secondary">Этаж: {place.floorName}. Тип места: {place.placeType.name}.</p>
              </div>
              <Button variant="outline-secondary" href={`/coworkings/${coworkingId}/settings/floors/${floorId}`}>
                Назад к местам
              </Button>
            </Stack>
          </Card.Body>
        </Card>

        <Card className="content-card">
          <Card.Body>
            <Stack direction="horizontal" gap={3} className="flex-wrap justify-content-between align-items-center mb-3">
              <Card.Title as="h2" className="h4 mb-0">Бронирования места</Card.Title>
              <Badge bg="secondary">{bookings.length}</Badge>
            </Stack>
            {actionMessage ? <Alert variant="success" dismissible
                                    onClose={() => setActionMessage(null)}>{actionMessage}</Alert> : null}
            {actionError ?
              <Alert variant="danger" dismissible onClose={() => setActionError(null)}>{actionError}</Alert> : null}
            <Table responsive hover>
              <thead>
              <tr>
                <th>Бронь</th>
                <th>Пользователь</th>
                <th>Дата</th>
                <th>Сумма</th>
                <th>Статус</th>
                <th className="text-end">Действия</th>
              </tr>
              </thead>
              <tbody>
              {bookings.length === 0 ? <tr>
                <td colSpan={6} className="text-center text-body-secondary py-4">Для этого места пока нет данных о
                  бронированиях.
                </td>
              </tr> : bookings.map((booking) => <tr key={booking.bookingNumber ?? booking.bookingId}>
                <td>{booking.bookingNumber ?? `#${booking.bookingId}`}</td>
                <td>{booking.userName}</td>
                <td>{formatBookingDate(booking.date)}</td>
                <td>{formatRublesFromKopecks(booking.cost)}</td>
                <td><Badge bg={booking.active ? 'success' : 'secondary'}>{formatBookingStatus(booking)}</Badge></td>
                <td className="text-end">
                  <Button
                    variant="outline-danger"
                    size="sm"
                    disabled={!booking.active || confirming}
                    onClick={() => void openCancelModal(booking)}>
                    Отменить через исключение
                  </Button>
                </td>
              </tr>)}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      </Stack>
    </Container>

    <Modal show={pendingClosing != null} onHide={() => !confirming && setPendingClosing(null)} size="xl" centered>
      <Modal.Header closeButton><Modal.Title>Подтвердить отмену брони через закрытие места</Modal.Title></Modal.Header>
      <Modal.Body>{impact ? <OperationalImpactPreview
        impact={impact}
        description={`После подтверждения будет создано закрытие места на ${pendingClosing?.date ?? 'выбранную дату'}. Затронутые бронирования будут отменены, компенсации начислятся автоматически.`}
      /> : null}</Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={() => setPendingClosing(null)}
                disabled={confirming}>Отмена</Button>
        <Button variant="danger" onClick={confirmCancelThroughClosing}
                disabled={confirming}>{confirming ? 'Выполняется...' : 'Подтвердить'}</Button>
      </Modal.Footer>
    </Modal>
  </main>;
}
