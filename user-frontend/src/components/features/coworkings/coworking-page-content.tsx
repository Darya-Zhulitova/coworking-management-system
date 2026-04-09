import Link from 'next/link';
import {getBookings, getCoworking, getMembership, getPlaces} from '@/lib/mock-data';
import {formatDate, formatMoney} from '@/lib/format';

export function CoworkingPageContent({coworkingId}: { coworkingId: number }) {
  const coworking = getCoworking(coworkingId);
  const membership = getMembership(coworkingId);
  const nearbyBookings = getBookings(coworkingId).filter((item) => item.active).slice(0, 2);
  const visiblePlaces = getPlaces(coworkingId).slice(0, 3);

  if (!coworking || !membership) {
    return <div className="alert alert-warning mb-0">Коворкинг не найден.</div>;
  }

  if (membership.status === 'pending') {
    return (
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4 p-lg-5 d-grid gap-4">
          <div>
            <h1 className="h3 mb-3">Membership ожидает подтверждения</h1>
            <p className="text-body-secondary mb-0">
              Пока членство не активировано, доступен только просмотр пространства и доступности мест без новых бронирований, финансовых запросов и сервисных заявок.
            </p>
          </div>
          <div className="row g-3">
            <div className="col-md-6"><div className="border rounded-4 p-3 h-100"><div className="text-body-secondary small mb-1">Адрес</div><div className="fw-semibold">{coworking.address}</div></div></div>
            <div className="col-md-6"><div className="border rounded-4 p-3 h-100"><div className="text-body-secondary small mb-1">Расписание</div><div className="fw-semibold">{coworking.scheduleLabel}</div></div></div>
          </div>
          <div className="card bg-body-tertiary border-0">
            <div className="card-body p-4">
              <h2 className="h5 mb-3">Предпросмотр доступности</h2>
              <div className="d-grid gap-3">
                {visiblePlaces.map((place) => (
                  <div key={place.id} className="d-flex justify-content-between gap-3 border rounded-4 p-3 bg-white">
                    <div>
                      <div className="fw-semibold">{place.name}</div>
                      <div className="small text-body-secondary">{place.floor} · {place.typeName}</div>
                    </div>
                    <div className={`small fw-semibold ${place.available ? 'text-success' : 'text-danger'}`}>{place.available ? 'Предварительно доступно' : 'Недоступно'}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>
    );
  }

  if (membership.status === 'blocked') {
    return (
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4 p-lg-5 d-grid gap-4">
          <div>
            <h1 className="h3 mb-3">Доступ в коворкинг заблокирован</h1>
            <p className="text-body-secondary mb-0">
              Можно вывести оставшиеся средства или заполнить сервисную заявку
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <Link href={`/coworkings/${coworkingId}/bookings`} className="btn btn-outline-secondary">История бронирований</Link>
            <Link href={`/coworkings/${coworkingId}/balance`} className="btn btn-outline-secondary">История операций</Link>
            <Link href={`/coworkings/${coworkingId}/requests`} className="btn btn-primary">Открыть заявки</Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm overflow-hidden">
        <div className="card-body p-4 p-lg-5">
          <div className="row g-4 align-items-center">
            <div className="col-lg-7">
              <h1 className="display-6 fw-semibold mb-3">{coworking.heroTitle}</h1>
              <p className="lead text-body-secondary mb-4">{coworking.heroText}</p>
              <div className="d-flex flex-wrap gap-2">
                <Link href={`/coworkings/${coworkingId}/bookings/new`} className="btn btn-primary btn-lg">Новое бронирование</Link>
                <Link href={`/coworkings/${coworkingId}/balance`} className="btn btn-outline-secondary btn-lg">Перейти к балансу</Link>
              </div>
            </div>
            <div className="col-lg-5">
              <div className="rounded-4 bg-body-tertiary p-4 h-100">
                <div className="text-body-secondary small mb-1">Адрес</div>
                <div className="fw-semibold mb-3">{coworking.address}</div>
                <div className="text-body-secondary small mb-1">Расписание</div>
                <div className="fw-semibold mb-0">{coworking.scheduleLabel}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="row g-4">
        <div className="col-12 col-xl-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                <div>
                  <h2 className="h5 mb-1">Доступность мест</h2>
                </div>
                <Link href={`/coworkings/${coworkingId}/bookings/new`} className="btn btn-sm btn-outline-primary">Открыть корзину</Link>
              </div>
              <div className="list-group list-group-flush">
                {visiblePlaces.map((place) => (
                  <div key={place.id} className="list-group-item px-0 py-3">
                    <div className="d-flex justify-content-between gap-3">
                      <div>
                        <div className="fw-semibold">{place.name}</div>
                        <div className="text-body-secondary small">{place.floor} · {place.typeName}</div>
                        <div className="small mt-2">{place.amenities.join(' · ')}</div>
                      </div>
                      <div className="text-end">
                        <div className="fw-semibold">{formatMoney(place.pricePerDay)}</div>
                        <div className={`small ${place.available ? 'text-success' : 'text-danger'}`}>
                          {place.available ? 'Доступно для добавления в корзину' : 'Недоступно'}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-grid gap-4">
              <div>
                <div className="text-body-secondary small">Текущий баланс</div>
                <div className="display-6 fw-semibold">{formatMoney(membership.balance)}</div>
              </div>
              <div>
                <h2 className="h5 mb-3">Ближайшие бронирования</h2>
                <div className="d-grid gap-3">
                  {nearbyBookings.map((booking) => (
                    <div className="border rounded-4 p-3" key={booking.id}>
                      <div className="fw-semibold">{booking.placeName}</div>
                      <div className="text-body-secondary small">{formatDate(booking.date)}</div>
                      <div className="mt-2">{formatMoney(booking.cost)}</div>
                    </div>
                  ))}
                </div>
              </div>
              <Link href={`/coworkings/${coworkingId}/bookings`} className="btn btn-outline-secondary">Все бронирования</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
