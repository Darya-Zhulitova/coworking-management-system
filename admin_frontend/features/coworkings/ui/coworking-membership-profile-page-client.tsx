'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
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
import { formatRublesFromKopecks } from '@/lib/format/money';
import type { MembershipProfileDto, OperationalImpactDto } from '@/types/place';

function statusLabel(value: string): string {
  const labels: Record<string, string> = {
    ACTIVE: 'Активный',
    PENDING: 'Ожидает подтверждения',
    BLOCKED: 'Заблокирован',
  };
  return labels[value] ?? value;
}

function statusVariant(value: string): string {
  if (value === 'ACTIVE') return 'success';
  if (value === 'PENDING') return 'warning';
  if (value === 'BLOCKED') return 'danger';
  return 'secondary';
}

function formatDate(value?: string | null): string {
  return value ? value.slice(0, 10) : '—';
}

export function CoworkingMembershipProfilePageClient({ coworkingId, membershipId }: {
  coworkingId: number;
  membershipId: number;
}) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true,
  });
  const [profile, setProfile] = useState<MembershipProfileDto | null>(null);
  const [amountRubles, setAmountRubles] = useState('');
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [blocking, setBlocking] = useState(false);
  const [blockPreview, setBlockPreview] = useState<OperationalImpactDto | null>(null);
  const [showBlockModal, setShowBlockModal] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const data = await requestJson<MembershipProfileDto>(`/api/coworkings/${coworkingId}/memberships/${membershipId}`);
    setProfile(data);
  }, [coworkingId, membershipId]);

  useEffect(() => {
    let mounted = true;
    load()
      .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Не удалось загрузить пользователя.'))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [load]);

  const amountMinorUnits = useMemo(() => {
    const normalized = amountRubles.replace(',', '.').trim();
    if (!normalized) return null;
    const value = Number(normalized);
    if (!Number.isFinite(value)) return null;
    return Math.round(value * 100);
  }, [amountRubles]);


  async function openBlockPreview() {
    if (!profile || profile.status === 'BLOCKED') return;
    setBlocking(true);
    setError(null);
    setMessage(null);
    try {
      const preview = await requestJson<OperationalImpactDto>(`/api/coworkings/${coworkingId}/memberships/${membershipId}/block-preview`, {
        method: 'POST',
      });
      setBlockPreview(preview);
      setShowBlockModal(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось подготовить предпросмотр блокировки пользователя.');
    } finally {
      setBlocking(false);
    }
  }

  async function confirmBlockMembership() {
    if (!blockPreview?.impactHash) {
      setError('Предпросмотр блокировки устарел. Обновите предпросмотр перед подтверждением.');
      return;
    }
    setBlocking(true);
    setError(null);
    setMessage(null);
    try {
      await requestJson<OperationalImpactDto>(`/api/coworkings/${coworkingId}/memberships/${membershipId}/block`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ impactHash: blockPreview.impactHash }),
      });
      await load();
      setShowBlockModal(false);
      setBlockPreview(null);
      setMessage('Пользователь заблокирован, активные бронирования отменены, компенсации начислены.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось заблокировать пользователя.');
    } finally {
      setBlocking(false);
    }
  }

  async function submitAdjustment(event: React.FormEvent) {
    event.preventDefault();
    if (amountMinorUnits == null || amountMinorUnits === 0) {
      setError('Введите ненулевую сумму корректировки.');
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const updated = await requestJson<MembershipProfileDto>(`/api/coworkings/${coworkingId}/memberships/${membershipId}/balance-adjustments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ amountMinorUnits, comment: comment.trim() || null }),
      });
      setProfile(updated);
      setAmountRubles('');
      setComment('');
      setMessage('Баланс скорректирован.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось скорректировать баланс.');
    } finally {
      setSaving(false);
    }
  }

  if (contextLoading || loading) return <FullPageLoader label="Загрузка профиля пользователя..."/>;
  if (contextError || error && !profile) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить профиль пользователя.'}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  if (!profile) return <FullPageError message="Пользователь не найден."/>;

  return <main className="page-shell">
    <Container className="py-4 py-md-5">
      <Stack gap={4}>
        <Card className="content-card">
          <Card.Body>
            <Stack direction="horizontal" gap={3} className="flex-wrap justify-content-between align-items-start">
              <div>
                <h1 className="h3 mb-2">{profile.userName}</h1>
                <p className="mb-0 text-body-secondary">{profile.userEmail ?? `пользователь #${profile.userId}`}</p>
              </div>
              <Stack direction="horizontal" gap={2} className="flex-wrap">
                {profile.status !== 'BLOCKED' ?
                  <Button variant="outline-danger" onClick={openBlockPreview} disabled={blocking}>
                    {blocking ? 'Подготовка...' : 'Заблокировать пользователя'}
                  </Button> : null}
                <Button variant="outline-secondary" href={`/coworkings/${coworkingId}/users/membership-list`}>Назад к
                  списку</Button>
              </Stack>
            </Stack>
          </Card.Body>
        </Card>

        {message ? <Alert variant="success" dismissible onClose={() => setMessage(null)}>{message}</Alert> : null}
        {error ? <Alert variant="danger" dismissible onClose={() => setError(null)}>{error}</Alert> : null}

        <Row className="g-4">
          <Col lg={5}>
            <Card className="content-card h-100">
              <Card.Body>
                <Card.Title as="h2" className="h4 mb-3">Профиль</Card.Title>
                <Stack gap={2}>
                  <div><span className="text-body-secondary">Статус:</span> <Badge
                    bg={statusVariant(profile.status)}>{statusLabel(profile.status)}</Badge></div>
                  <div><span className="text-body-secondary">Создан:</span> {formatDate(profile.createdAt)}</div>
                  <div><span className="text-body-secondary">Подтвержден:</span> {formatDate(profile.approvedAt)}</div>
                  <div><span className="text-body-secondary">Заблокирован:</span> {formatDate(profile.blockedAt)}</div>
                  <div><span className="text-body-secondary">Баланс:</span>
                    <strong>{formatRublesFromKopecks(profile.balanceMinorUnits)}</strong></div>
                  {profile.userDescription ?
                    <div><span className="text-body-secondary">Описание:</span><br/>{profile.userDescription}
                    </div> : null}
                </Stack>
              </Card.Body>
            </Card>
          </Col>
          <Col lg={7}>
            <Card className="content-card h-100">
              <Card.Body>
                <Card.Title as="h2" className="h4 mb-3">Ручная корректировка баланса</Card.Title>
                <Form onSubmit={submitAdjustment}>
                  <Stack gap={3}>
                    <Form.Group>
                      <Form.Label>Сумма в рублях</Form.Label>
                      <Form.Control value={amountRubles} onChange={(event) => setAmountRubles(event.target.value)}
                                    placeholder="Например: 1500 или -500" required/>
                    </Form.Group>
                    <Form.Group>
                      <Form.Label>Комментарий администратора</Form.Label>
                      <Form.Control as="textarea" rows={3} value={comment}
                                    onChange={(event) => setComment(event.target.value)} maxLength={1000}/>
                    </Form.Group>
                    <Button type="submit"
                            disabled={saving}>{saving ? 'Сохранение...' : 'Скорректировать баланс'}</Button>
                  </Stack>
                </Form>
              </Card.Body>
            </Card>
          </Col>
        </Row>

        <Card className="content-card">
          <Card.Body>
            <Stack direction="horizontal" gap={3} className="flex-wrap justify-content-between align-items-center mb-3">
              <Card.Title as="h2" className="h4 mb-0">Бронирования</Card.Title>
            </Stack>
            <Table responsive hover>
              <thead>
              <tr>
                <th>Бронь</th>
                <th>Место</th>
                <th>Дата</th>
                <th>Сумма</th>
                <th>Статус</th>
              </tr>
              </thead>
              <tbody>
              {profile.activeBookings.length === 0 ? <tr>
                <td colSpan={5} className="text-center text-body-secondary py-4">Активных бронирований нет.</td>
              </tr> : profile.activeBookings.map((booking) => <tr key={booking.bookingNumber ?? booking.bookingId}>
                <td>{booking.bookingNumber ?? `#${booking.bookingId}`}</td>
                <td>{booking.placeName}</td>
                <td>{formatDate(booking.date)}</td>
                <td>{formatRublesFromKopecks(booking.cost)}</td>
                <td>
                  {{
                    ACTUAL: 'Активно',
                    CANCELED_ADMIN: 'Отменено администратором',
                    CANCELED_USER: 'Отменено пользователем',
                  }[booking.status]}
                </td>
              </tr>)}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      </Stack>

      <Modal show={showBlockModal} onHide={() => !blocking && setShowBlockModal(false)} size="lg" centered>
        <Modal.Header closeButton={!blocking}>
          <Modal.Title>Подтвердить блокировку пользователя</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {blockPreview ? <Stack gap={3}>
            <Alert variant="warning" className="mb-0">
              После подтверждения пользователь будет заблокирован. Все активные бронирования будут отменены, а средства
              за неиспользованные бронирования автоматически возвращены на баланс пользователя.
            </Alert>
            <Row className="g-3">
              <Col md={4}>
                <div className="border rounded-3 p-3 h-100">
                  <div className="text-body-secondary small">Бронирований к отмене</div>
                  <div className="h4 mb-0">{blockPreview.affectedBookingsCount}</div>
                </div>
              </Col>
              <Col md={4}>
                <div className="border rounded-3 p-3 h-100">
                  <div className="text-body-secondary small">Всего компенсаций</div>
                  <div className="h4 mb-0">{formatRublesFromKopecks(blockPreview.totalCompensationAmount)}</div>
                </div>
              </Col>
              <Col md={4}>
                <div className="border rounded-3 p-3 h-100">
                  <div className="text-body-secondary small">Затронутые даты</div>
                  <div
                    className="mb-0">{blockPreview.affectedDates.length ? blockPreview.affectedDates.join(', ') : '—'}</div>
                </div>
              </Col>
            </Row>
            <Table responsive hover className="mb-0">
              <thead>
              <tr>
                <th>Бронь</th>
                <th>Место</th>
                <th>Дата</th>
                <th>Стоимость</th>
                <th>Возврат</th>
              </tr>
              </thead>
              <tbody>
              {blockPreview.affectedBookings.length === 0 ? <tr>
                <td colSpan={5} className="text-center text-body-secondary py-4">Активных бронирований для отмены нет.
                </td>
              </tr> : blockPreview.affectedBookings.map((booking) => <tr key={booking.bookingId}>
                <td>{booking.bookingNumber ?? `#${booking.bookingId}`}</td>
                <td>{booking.placeName}</td>
                <td>{formatDate(booking.date)}</td>
                <td>{formatRublesFromKopecks(booking.bookingAmount)}</td>
                <td>{formatRublesFromKopecks(booking.compensationAmount)}</td>
              </tr>)}
              </tbody>
            </Table>
          </Stack> : <FullPageLoader label="Подготовка превью..."/>}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setShowBlockModal(false)}
                  disabled={blocking}>Отмена</Button>
          <Button variant="danger" onClick={confirmBlockMembership} disabled={blocking || !blockPreview}>
            {blocking ? 'Блокировка...' : 'Подтвердить блокировку'}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  </main>;
}
