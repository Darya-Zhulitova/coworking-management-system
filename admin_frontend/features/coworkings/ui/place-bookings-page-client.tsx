'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import { formatRublesFromKopecks } from '@/lib/format/money';
import type { PlaceBookingListResponseDto, PlaceDto } from '@/types/place';

function formatBookingDate(value: string): string {
  if (!value) return '—';
  return value.slice(0, 10);
}

function formatBookingStatus(booking: { status: string; active: boolean }): string {
  if (!booking.active) return 'Неактивна';
  const labels: Record<string, string> = {
    active: 'Активна',
    ACTIVE: 'Активна',
    canceled: 'Отменена',
    CANCELED: 'Отменена',
    CANCELED_ADMIN: 'Отменена администратором',
  };
  return labels[booking.status] ?? booking.status;
}

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
            <Table responsive hover>
              <thead>
              <tr>
                <th>Бронь</th>
                <th>Пользователь</th>
                <th>Дата</th>
                <th>Сумма</th>
                <th>Статус</th>
              </tr>
              </thead>
              <tbody>
              {bookings.length === 0 ? <tr>
                <td colSpan={5} className="text-center text-body-secondary py-4">Для этого места пока нет данных о
                  бронированиях.
                </td>
              </tr> : bookings.map((booking) => <tr key={booking.bookingId}>
                <td>#{booking.bookingId}</td>
                <td>{booking.userName}<br/><span
                  className="text-body-secondary">membership #{booking.membershipId}</span></td>
                <td>{formatBookingDate(booking.date)}</td>
                <td>{formatRublesFromKopecks(booking.cost)}</td>
                <td><Badge bg={booking.active ? 'success' : 'secondary'}>{formatBookingStatus(booking)}</Badge></td>
              </tr>)}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      </Stack>
    </Container>
  </main>;
}
