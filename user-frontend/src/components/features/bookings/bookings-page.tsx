'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatDate, formatDateTimeShort, formatMoney, formatMoneyCompact } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { Booking, BookingInitData } from '@/lib/types';
import { notifyCoworkingContextChanged } from '@/components/layout/coworking-shell-context';

type CancelBookingResult = {
  bookingId: number;
  coworkingId: number;
  refundMinorUnits: number;
  balanceAfterMinorUnits: number;
};


function getBookingStart(date: string): Date {
  return new Date(`${date}T00:00:00`);
}

function getBookingEnd(date: string): Date {
  return new Date(`${date}T23:59:59`);
}

function isBookingCurrentlyActive(booking: Booking): boolean {
  return booking.status === 'ACTUAL' && getBookingEnd(booking.date).getTime() > Date.now();
}

function getBookingDisplayStatus(booking: Booking): string {
  if (booking.status === 'CANCELED_ADMIN') return 'Отменена администратором';
  if (booking.status === 'CANCELED_USER') return 'Отменена пользователем';
  return isBookingCurrentlyActive(booking) ? 'Актуальная' : 'Завершена';
}

function getCancellationCaption(booking: Booking): string {
  const currentRefund = booking.cancellationPreview ?? 0;
  if (currentRefund <= 0) return 'Отмена по правилам тарифа больше недоступна.';

  const fullRefundAmount = booking.cost;
  const lateRefundAmount = Math.trunc((booking.cost * booking.lateCancellationRefundPercent) / 100);
  const cutoffAt = new Date(getBookingStart(booking.date).getTime() - booking.fullRefundHoursBefore * 60 * 60 * 1000);

  if (currentRefund >= fullRefundAmount && booking.lateCancellationRefundPercent < 100) {
    return `Возврат ${formatMoneyCompact(fullRefundAmount)}. После ${formatDateTimeShort(cutoffAt)} возврат ${formatMoneyCompact(lateRefundAmount)} (${booking.lateCancellationRefundPercent}%)`;
  }

  if (currentRefund >= fullRefundAmount) {
    return `Возврат ${formatMoneyCompact(fullRefundAmount)}.`;
  }

  return `Возврат ${formatMoneyCompact(currentRefund)} (${booking.lateCancellationRefundPercent}%).`;
}

export function BookingsPage({ coworkingId }: { coworkingId: number }) {
  const [membershipStatus, setMembershipStatus] = useState<string | null>(null);
  const [items, setItems] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [bookingToCancel, setBookingToCancel] = useState<Booking | null>(null);

  const loadBookings = useCallback(async () => {
    const [initData, bookings] = await Promise.all([
      requestJson<BookingInitData>(`/api/coworkings/${coworkingId}/booking/init`),
      requestJson<Booking[]>(`/api/coworkings/${coworkingId}/bookings`),
    ]);
    setMembershipStatus(initData.membershipStatus);
    setItems(bookings);
    setErrorMessage(null);
  }, [coworkingId]);

  useEffect(() => {
    let active = true;
    loadBookings()
      .catch((error: unknown) => {
        if (!active) return;
        setErrorMessage(error instanceof ClientRequestError ? error.message : 'Не удалось загрузить бронирования.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [loadBookings]);

  useEffect(() => {
    if (!bookingToCancel) {
      document.body.classList.remove('modal-open');
      document.body.style.removeProperty('overflow');
      document.body.style.removeProperty('padding-right');
      return;
    }

    document.body.classList.add('modal-open');
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.classList.remove('modal-open');
      document.body.style.removeProperty('overflow');
      document.body.style.removeProperty('padding-right');
    };
  }, [bookingToCancel]);

  const activeItems = useMemo(() => items.filter((item) => isBookingCurrentlyActive(item)), [items]);
  const historyItems = useMemo(() => items.filter((item) => !isBookingCurrentlyActive(item)), [items]);
  const canCreate = membershipStatus === 'active';

  const cancellationDetails = useMemo(() => {
    if (!bookingToCancel) {
      return null;
    }

    const refundAmount = bookingToCancel.cancellationPreview ?? 0;
    return {
      refundAmount,
      refundRule: refundAmount >= bookingToCancel.cost
        ? 'Полный возврат.'
        : `Частичный возврат ${bookingToCancel.lateCancellationRefundPercent}%.`,
    };
  }, [bookingToCancel]);

  function openCancelModal(booking: Booking) {
    if (membershipStatus !== 'active') return;
    if ((booking.cancellationPreview ?? 0) <= 0) return;
    setBookingToCancel(booking);
    setActionMessage(null);
    setErrorMessage(null);
  }

  function closeCancelModal() {
    if (cancellingId !== null) return;
    setBookingToCancel(null);
  }

  async function confirmCancel() {
    if (!bookingToCancel || membershipStatus !== 'active') return;

    setCancellingId(bookingToCancel.id);
    setActionMessage(null);
    try {
      const response = await requestJson<CancelBookingResult>(`/api/coworkings/${coworkingId}/bookings/${bookingToCancel.id}/cancel`, { method: 'POST' });
      notifyCoworkingContextChanged();
      await loadBookings();
      setActionMessage(`Бронирование отменено. Возврат: ${formatMoney(response.refundMinorUnits)}.`);
      setBookingToCancel(null);
    } catch (error: unknown) {
      setErrorMessage(error instanceof ClientRequestError ? error.message : 'Не удалось отменить бронирование.');
    } finally {
      setCancellingId(null);
    }
  }

  if (loading) return <div className="alert alert-light border mb-0">Загрузка бронирований...</div>;
  if (errorMessage && !items.length) return <div className="alert alert-danger mb-0">{errorMessage}</div>;

  return (
    <>
      <div className="d-grid gap-4">
        <section className="card border-0 shadow-sm">
          <div className="card-body p-4">
            <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
              <div>
                <h1 className="h3 mb-2">Бронирования</h1>
                <div className="text-body-secondary">Управляйте активными бронированиями и просматривайте историю.</div>
              </div>
              <Link href={canCreate ? `/coworkings/${coworkingId}/bookings/new` : `/coworkings/${coworkingId}`}
                    className={`btn ${canCreate ? 'btn-primary' : 'btn-outline-secondary disabled'}`}>
                Новое бронирование
              </Link>
            </div>
          </div>
        </section>

        {errorMessage ? <div className="alert alert-danger mb-0">{errorMessage}</div> : null}
        {actionMessage ? <div className="alert alert-success mb-0">{actionMessage}</div> : null}

        <div className="row g-4">
          <div className="col-12 col-xl-6">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body p-4 d-grid gap-3">
                <h2 className="h5 mb-0">Активные</h2>
                {activeItems.length === 0 ? (
                  <div className="text-body-secondary">Активных бронирований пока нет.</div>
                ) : activeItems.map((booking) => {
                  const cancellationAvailable = membershipStatus === 'active' && (booking.cancellationPreview ?? 0) > 0;
                  const isCancelling = cancellingId === booking.id;

                  return (
                    <div className="card border" key={booking.id}>
                      <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                          <div>
                            <div className="fw-semibold">{booking.placeName}</div>
                            <div className="small text-body-secondary">{formatDate(booking.date)} ·
                              Заказ {booking.requestId}</div>
                            <div className="small text-body-secondary">Статус: {getBookingDisplayStatus(booking)}</div>
                          </div>
                          <div className="fw-semibold">{formatMoney(booking.cost)}</div>
                        </div>

                        <div className="d-flex justify-content-between align-items-center gap-3 pt-3 border-top">
                          <div className="small text-body-secondary">
                            {getCancellationCaption(booking)}
                          </div>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            onClick={() => openCancelModal(booking)}
                            disabled={!cancellationAvailable || isCancelling}
                          >
                            {isCancelling ? 'Отмена...' : 'Отменить'}
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="col-12 col-xl-6">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body p-4 d-grid gap-3">
                <h2 className="h5 mb-0">История</h2>
                {historyItems.length === 0 ? (
                  <div className="text-body-secondary">История бронирований пока пуста.</div>
                ) : historyItems.map((booking) => (
                  <div className="card border" key={booking.id}>
                    <div className="card-body">
                      <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                        <div>
                          <div className="fw-semibold">{booking.placeName}</div>
                          <div className="small text-body-secondary">{formatDate(booking.date)} ·
                            Заказ {booking.requestId}</div>
                        </div>
                        <div className="fw-semibold">{formatMoney(booking.cost)}</div>
                      </div>
                      <div className="small text-body-secondary">Статус: {getBookingDisplayStatus(booking)}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {bookingToCancel && cancellationDetails ? (
        <>
          <div
            className="modal fade show"
            tabIndex={-1}
            role="dialog"
            aria-modal="true"
            aria-labelledby="cancelBookingModalTitle"
            style={{ display: 'block' }}
            onClick={closeCancelModal}
          >
            <div className="modal-dialog modal-dialog-centered" onClick={(event) => event.stopPropagation()}>
              <div className="modal-content">
                <div className="modal-header">
                  <h1 className="modal-title fs-5" id="cancelBookingModalTitle">Подтверждение отмены</h1>
                  <button
                    type="button"
                    className="btn-close"
                    aria-label="Закрыть"
                    onClick={closeCancelModal}
                    disabled={cancellingId !== null}
                  />
                </div>
                <div className="modal-body d-grid gap-2">
                  <p className="mb-0">Отменить
                    бронирование {bookingToCancel.placeName} на {formatDate(bookingToCancel.date)}?</p>
                  <p className="mb-0 text-body-secondary">{cancellationDetails.refundRule}</p>
                  <div className="fw-semibold">Сумма возврата: {formatMoney(cancellationDetails.refundAmount)}</div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={closeCancelModal}
                          disabled={cancellingId !== null}>
                    Закрыть
                  </button>
                  <button type="button" className="btn btn-danger" onClick={() => void confirmCancel()}
                          disabled={cancellingId !== null}>
                    {cancellingId !== null ? 'Отмена...' : 'Подтвердить отмену'}
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show"/>
        </>
      ) : null}
    </>
  );
}
