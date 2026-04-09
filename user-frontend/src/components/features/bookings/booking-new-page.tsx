'use client';

import {useEffect, useMemo, useState} from 'react';
import Link from 'next/link';
import {BookingCartItem, Place} from '@/lib/types';
import {calculateCart, getMembership, getPlaces} from '@/lib/mock-data';
import {formatDate, formatMoney} from '@/lib/format';

function cartKey(coworkingId: number): string {
  return `booking_cart_${coworkingId}`;
}

function listDates(start: string, end: string): string[] {
  if (!start || !end || start > end) {
    return [];
  }
  const result: string[] = [];
  const current = new Date(`${start}T00:00:00`);
  const last = new Date(`${end}T00:00:00`);
  while (current <= last) {
    result.push(current.toISOString().slice(0, 10));
    current.setDate(current.getDate() + 1);
  }
  return result;
}

export function BookingNewPage({coworkingId}: { coworkingId: number }) {
  const membership = getMembership(coworkingId);
  const places = getPlaces(coworkingId);
  const [date, setDate] = useState('2026-04-23');
  const [typeFilter, setTypeFilter] = useState('all');
  const [floorFilter, setFloorFilter] = useState('all');
  const [priceMax, setPriceMax] = useState('');
  const [cart, setCart] = useState<BookingCartItem[]>([]);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(cartKey(coworkingId));
      if (!raw) {
        return;
      }
      const parsed = JSON.parse(raw) as {coworkingId?: number; items?: BookingCartItem[]};
      if (parsed.coworkingId === coworkingId && Array.isArray(parsed.items)) {
        setCart(parsed.items);
      }
    } catch {
      setCart([]);
    }
  }, [coworkingId]);

  useEffect(() => {
    window.localStorage.setItem(cartKey(coworkingId), JSON.stringify({coworkingId, items: cart}));
  }, [cart, coworkingId]);

  const availableTypes = Array.from(new Set(places.map((place) => place.typeName)));
  const availableFloors = Array.from(new Set(places.map((place) => place.floor)));
  const filteredPlaces = places.filter((place) => {
    const byType = typeFilter === 'all' || place.typeName === typeFilter;
    const byFloor = floorFilter === 'all' || place.floor === floorFilter;
    const byPrice = !priceMax || place.pricePerDay <= Number(priceMax) * 100;
    return byType && byFloor && byPrice;
  });
  const calculation = useMemo(() => calculateCart(coworkingId, cart), [cart, coworkingId]);
  const selectedDates = listDates(date, dateTo);
  const unavailableItems = calculation.items.filter((item) => !item.available).length;
  const canCheckout = membership?.status === 'active' && cart.length > 0 && unavailableItems === 0 && (membership.balance ?? 0) >= calculation.summary.totalFinalPrice;

  function addPlace(place: Place): void {
    const nextItems = [...cart];
    selectedDates.forEach((date) => {
      const exists = nextItems.some((item) => item.placeId === place.id && item.date === date);
      if (!exists) {
        nextItems.push({placeId: place.id, date});
      }
    });
    setCart(nextItems);
  }

  function removeItem(item: BookingCartItem): void {
    setCart(cart.filter((candidate) => !(candidate.placeId === item.placeId && candidate.date === item.date)));
  }

  function clearCart(): void {
    setCart([]);
  }

  if (membership?.status !== 'active') {
    return (
      <div className="card border-0 shadow-sm">
        <div className="card-body p-4 d-grid gap-3">
          <h1 className="h4 mb-0">Новое бронирование недоступно</h1>
          <div><Link href={`/coworkings/${coworkingId}`} className="btn btn-primary">Вернуться в коворкинг</Link></div>
        </div>
      </div>
    );
  }

  return (
    <div className="d-grid gap-4">
      <section>
        <h1 className="h3 mb-2">Новое бронирование</h1>
      </section>

      <div className="row g-4 align-items-start">
        <div className="col-12 col-xl-3">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <h2 className="h5 mb-0">Фильтры</h2>
              <div>
                <label className="form-label">Дата</label>
                <input className="form-control" type="date" value={date} onChange={(event) => setDate(event.target.value)}/>
              </div>
              <div>
                <label className="form-label">Тип места</label>
                <select className="form-select" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
                  <option value="all">Все типы</option>
                  {availableTypes.map((item) => <option key={item} value={item}>{item}</option>)}
                </select>
              </div>
              <div>
                <label className="form-label">Этаж</label>
                <select className="form-select" value={floorFilter} onChange={(event) => setFloorFilter(event.target.value)}>
                  <option value="all">Все этажи</option>
                  {availableFloors.map((item) => <option key={item} value={item}>{item}</option>)}
                </select>
              </div>
              <div>
                <label className="form-label">Цена до, ₽</label>
                <input className="form-control" inputMode="numeric" value={priceMax} onChange={(event) => setPriceMax(event.target.value)} placeholder="Например, 3000"/>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <h2 className="h5 mb-1">Список мест</h2>
                </div>
                <div className="small text-body-secondary">{selectedDates.length} дн.</div>
              </div>
              {filteredPlaces.map((place) => (
                <div className="border rounded-4 p-3" key={place.id}>
                  <div className="d-flex justify-content-between gap-3">
                    <div>
                      <div className="fw-semibold">{place.name}</div>
                      <div className="small text-body-secondary">{place.floor} · {place.typeName}</div>
                      <div className="small mt-2">{place.amenities.join(' · ')}</div>
                    </div>
                    <div className="text-end">
                      <div className="fw-semibold">{formatMoney(place.pricePerDay)}</div>
                      <div className={`small ${place.available ? 'text-success' : 'text-danger'}`}>{place.available ? 'Можно добавить' : 'Недоступно'}</div>
                    </div>
                  </div>
                  <div className="d-flex justify-content-end mt-3">
                    <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => addPlace(place)} disabled={selectedDates.length === 0 || !place.available}>
                      Добавить в корзину
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-4">
          <div className="card border-0 shadow-sm sticky-top" style={{top: '6rem'}}>
            <div className="card-body p-4 d-grid gap-3">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <h2 className="h5 mb-1">Корзина</h2>
                </div>
                <button type="button" className="btn btn-link btn-sm text-decoration-none p-0" onClick={clearCart}>Очистить</button>
              </div>

              {cart.length === 0 ? <div className="text-body-secondary">Корзина пуста. Добавьте место и диапазон дат.</div> : (
                <div className="d-grid gap-3">
                  {calculation.items.map((item) => (
                    <div key={`${item.placeId}-${item.date}`} className={`border rounded-4 p-3 ${item.available ? '' : 'border-danger-subtle bg-danger-subtle'}`}>
                      <div className="d-flex justify-content-between gap-3 mb-2">
                        <div>
                          <div className="fw-semibold">{item.placeName}</div>
                          <div className="small text-body-secondary">{item.floor} · {item.typeName}</div>
                        </div>
                        <button type="button" className="btn-close" aria-label="Удалить" onClick={() => removeItem({placeId: item.placeId, date: item.date})}/>
                      </div>
                      <div className="small mb-2">{formatDate(item.date)}</div>
                      <div className="d-flex justify-content-between small"><span>Базовая цена</span><span>{formatMoney(item.basePrice)}</span></div>
                      <div className="d-flex justify-content-between small"><span>Скидка {item.discountPercent}%</span><span>-{formatMoney(item.discountAmount)}</span></div>
                      <div className="d-flex justify-content-between fw-semibold mt-2"><span>Итог</span><span>{formatMoney(item.finalPrice)}</span></div>
                      {!item.available && <div className="small text-danger mt-2">Позиция больше недоступна и не пройдёт финальную оплату.</div>}
                    </div>
                  ))}
                </div>
              )}

              <div className="border-top pt-3 d-grid gap-2">
                <div className="d-flex justify-content-between"><span className="text-body-secondary">Базовая сумма</span><span>{formatMoney(calculation.summary.totalBasePrice)}</span></div>
                <div className="d-flex justify-content-between"><span className="text-body-secondary">Сумма скидки</span><span>-{formatMoney(calculation.summary.totalDiscount)}</span></div>
                <div className="d-flex justify-content-between fw-semibold fs-5"><span>Итого</span><span>{formatMoney(calculation.summary.totalFinalPrice)}</span></div>
                {calculation.summary.discountHints.map((hint) => <div key={hint} className="small text-body-secondary">{hint}</div>)}
                {unavailableItems > 0 && <div className="alert alert-warning py-2 mb-0">Перед оплатой уберите недоступные позиции.</div>}
                {(membership.balance ?? 0) < calculation.summary.totalFinalPrice && cart.length > 0 && <div className="alert alert-danger py-2 mb-0">Недостаточно средств. Баланс после списания должен оставаться неотрицательным.</div>}
                <div className="small text-body-secondary">Оплата проверит доступность и цену повторно. Корзина не резервирует место.</div>
                <button type="button" className="btn btn-primary btn-lg" disabled={!canCheckout}>Оплатить</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
