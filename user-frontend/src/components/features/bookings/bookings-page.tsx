import Link from 'next/link';
import {getBookings, getMembership} from '@/lib/mock-data';
import {formatDate, formatMoney} from '@/lib/format';

export function BookingsPage({coworkingId}: { coworkingId: number }) {
  const membership = getMembership(coworkingId);
  const items = getBookings(coworkingId);
  const activeItems = items.filter((item) => item.active);
  const historyItems = items.filter((item) => !item.active);
  const canCreate = membership?.status === 'active';

  return (
    <div className="d-grid gap-4">
      <section className="d-flex flex-wrap justify-content-between align-items-start gap-3">
        <div>
          <h1 className="h3 mb-2">Бронирования</h1>
        </div>
        <Link href={canCreate ? `/coworkings/${coworkingId}/bookings/new` : `/coworkings/${coworkingId}`} className={`btn ${canCreate ? 'btn-primary' : 'btn-outline-secondary disabled'}`}>
          Новое бронирование
        </Link>
      </section>

      <div className="row g-4">
        <div className="col-12 col-xl-6">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-grid gap-3">
              <h2 className="h5 mb-0">Активные</h2>
              {activeItems.map((booking) => (
                <div className="border rounded-4 p-3" key={booking.id}>
                  <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                    <div>
                      <div className="fw-semibold">{booking.placeName}</div>
                      <div className="small text-body-secondary">{formatDate(booking.date)} · Оформление {booking.requestId}</div>
                    </div>
                    <div className="fw-semibold">{formatMoney(booking.cost)}</div>
                  </div>
                  <div className="d-flex justify-content-between align-items-center gap-3 mt-3 pt-3 border-top">
                    <div className="small text-body-secondary">
                      Возврат сейчас: {formatMoney(booking.cancellationPreview ?? 0)}
                    </div>
                    <button type="button" className="btn btn-outline-danger btn-sm">Отменить</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-6">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-grid gap-3">
              <h2 className="h5 mb-0">История</h2>
              {historyItems.map((booking) => (
                <div className="border rounded-4 p-3" key={booking.id}>
                  <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                    <div>
                      <div className="fw-semibold">{booking.placeName}</div>
                      <div className="small text-body-secondary">{formatDate(booking.date)} · Оформление {booking.requestId}</div>
                    </div>
                    <div className="fw-semibold">{formatMoney(booking.cost)}</div>
                  </div>
                  <div className="small text-body-secondary">{booking.active ? 'Активно' : 'Неактивно'}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
