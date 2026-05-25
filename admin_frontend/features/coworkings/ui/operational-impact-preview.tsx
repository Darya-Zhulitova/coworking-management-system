'use client';

import { useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { formatRublesFromKopecks } from '@/lib/format/money';
import type { OperationalImpactDto } from '@/types/place';

type OperationalImpactPreviewProps = {
  impact: OperationalImpactDto;
  description: string;
};

const PREVIEW_DATES_LIMIT = 4;

function formatDisplayDate(value: string): string {
  const [year, month, day] = value.split('-');
  if (!year || !month || !day) return value;
  return `${day}.${month}.${year}`;
}

function formatDates(dates: string[], showAll: boolean): string {
  if (dates.length === 0) return 'нет';
  const visibleDates = showAll ? dates : dates.slice(0, PREVIEW_DATES_LIMIT);
  const formatted = visibleDates.map(formatDisplayDate).join(', ');
  const hiddenCount = dates.length - visibleDates.length;
  return hiddenCount > 0 ? `${formatted} и еще ${hiddenCount}` : formatted;
}

export function OperationalImpactPreview({ impact, description }: OperationalImpactPreviewProps) {
  const [showAllDates, setShowAllDates] = useState(false);
  const affectedUsersCount = useMemo(() => new Set(impact.affectedBookings.map((booking) => booking.userId)).size, [impact.affectedBookings]);
  const hasHiddenDates = impact.affectedDates.length > PREVIEW_DATES_LIMIT;

  return <Stack gap={3}>
    <Alert variant="primary" className="mb-0">
      {description}
    </Alert>

    <Row className="g-3">
      <Col md={4}>
        <Card className="h-100 border">
          <Card.Body className="py-3">
            <div className="text-body-secondary small">Бронирований</div>
            <div className="fs-4 fw-semibold">{impact.affectedBookingsCount}</div>
          </Card.Body>
        </Card>
      </Col>
      <Col md={4}>
        <Card className="h-100 border">
          <Card.Body className="py-3">
            <div className="text-body-secondary small">Пользователей</div>
            <div className="fs-4 fw-semibold">{affectedUsersCount}</div>
          </Card.Body>
        </Card>
      </Col>
      <Col md={4}>
        <Card className="h-100 border">
          <Card.Body className="py-3">
            <div className="text-body-secondary small">Компенсаций</div>
            <div className="fs-4 fw-semibold">{formatRublesFromKopecks(impact.totalCompensationAmount)}</div>
          </Card.Body>
        </Card>
      </Col>
    </Row>

    <div className="small text-body-secondary">
      <span className="fw-semibold text-body">Затронутые даты: </span>
      {formatDates(impact.affectedDates, showAllDates)}
      {hasHiddenDates ? <Button
        variant="link"
        size="sm"
        className="p-0 ms-2 align-baseline"
        onClick={() => setShowAllDates((current) => !current)}>
        {showAllDates ? 'Свернуть' : 'Показать все'}
      </Button> : null}
    </div>

    <Table responsive bordered hover size="sm" className="align-middle mb-0">
      <thead>
      <tr>
        <th>Бронь</th>
        <th>Пользователь</th>
        <th>Место</th>
        <th>Дата</th>
        <th>Стоимость</th>
        <th>Возврат</th>
      </tr>
      </thead>
      <tbody>{impact.affectedBookings.length > 0 ? impact.affectedBookings.map((booking) => <tr
        key={booking.bookingNumber ?? booking.bookingId}>
        <td>{booking.bookingNumber ?? `#${booking.bookingId}`}</td>
        <td>{booking.userName}</td>
        <td>{booking.placeName}</td>
        <td>{formatDisplayDate(booking.date)}</td>
        <td>{formatRublesFromKopecks(booking.bookingAmount)}</td>
        <td>{formatRublesFromKopecks(booking.compensationAmount)}</td>
      </tr>) : <tr>
        <td colSpan={6} className="text-center text-body-secondary py-4">Нет затронутых бронирований</td>
      </tr>}</tbody>
    </Table>

    <div className="text-end fw-semibold">
      Итого: {impact.affectedBookingsCount} бронирований
      · {formatRublesFromKopecks(impact.totalCompensationAmount)} компенсаций
    </div>
  </Stack>;
}
