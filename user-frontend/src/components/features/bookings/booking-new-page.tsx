'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { formatDate, formatMoney } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type {
  BookingCartItem,
  BookingInitData,
  BookingInitPlace,
  CartCalculatedItem,
  CartCalculation
} from '@/lib/types';
import { notifyCoworkingContextChanged } from '@/components/layout/coworking-shell-context';

function cartKey(coworkingId: number): string {
  return `booking_cart_${coworkingId}`;
}

function normalizeCart(items: BookingCartItem[]): BookingCartItem[] {
  const unique = new Map<string, BookingCartItem>();
  for (const item of items) unique.set(`${item.placeId}-${item.date}`, item);
  return Array.from(unique.values());
}

function readStoredCart(coworkingId: number): BookingCartItem[] {
  try {
    const raw = window.localStorage.getItem(cartKey(coworkingId));
    if (!raw) return [];
    const parsed = JSON.parse(raw) as { coworkingId?: number; items?: BookingCartItem[] };
    return parsed.coworkingId === coworkingId && Array.isArray(parsed.items) ? normalizeCart(parsed.items) : [];
  } catch {
    return [];
  }
}

function emptyCalculation(coworkingId: number, balance = 0): CartCalculation {
  return {
    coworkingId,
    items: [],
    summary: {
      totalBasePrice: 0,
      totalDiscount: 0,
      totalFinalPrice: 0,
      unavailableCount: 0,
      discountHints: [],
      validationErrors: [],
      hasEnoughBalance: true,
      balanceAfterMinorUnits: balance,
      canCheckout: false,
    },
  };
}

export function BookingNewPage({ coworkingId }: { coworkingId: number }) {
  const router = useRouter();
  const [initData, setInitData] = useState<BookingInitData | null>(null);
  const [cart, setCart] = useState<BookingCartItem[]>([]);
  const [calculation, setCalculation] = useState<CartCalculation>(emptyCalculation(coworkingId));
  const [selectedDate, setSelectedDate] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [floorFilter, setFloorFilter] = useState('all');
  const [priceMax, setPriceMax] = useState('');
  const [loading, setLoading] = useState(true);
  const [calcLoading, setCalcLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [cartHydrated, setCartHydrated] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadInitData = useCallback(async (date: string | null, preserveSelection: boolean): Promise<BookingInitData> => {
    const query = date ? `?date=${encodeURIComponent(date)}` : '';
    const data = await requestJson<BookingInitData>(`/api/coworkings/${coworkingId}/booking/init${query}`);
    setInitData(data);
    setCalculation((current) => {
      if (current.items.length > 0 || current.summary.totalFinalPrice > 0) {
        return {
          ...current,
          summary: {
            ...current.summary,
            balanceAfterMinorUnits: data.balanceMinorUnits - current.summary.totalFinalPrice,
            hasEnoughBalance: data.balanceMinorUnits >= current.summary.totalFinalPrice,
            canCheckout: current.summary.canCheckout && data.balanceMinorUnits >= current.summary.totalFinalPrice,
          },
        };
      }
      return emptyCalculation(coworkingId, data.balanceMinorUnits);
    });
    if (!preserveSelection || !date) {
      setSelectedDate(data.previewDate);
    }
    return data;
  }, [coworkingId]);

  const recalculateCart = useCallback(async (items: BookingCartItem[], balanceMinorUnits: number): Promise<void> => {
    if (!items.length) {
      setCalculation(emptyCalculation(coworkingId, balanceMinorUnits));
      return;
    }

    // setCalcLoading(true);
    try {
      const data = await requestJson<CartCalculation>('/api/bookings/cart/calculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coworkingId, items }),
      });
      setCalculation(data);
      setErrorMessage(null);
    } catch (error: unknown) {
      setErrorMessage(error instanceof ClientRequestError ? error.message : 'Не удалось пересчитать состав бронирования.');
    } finally {
      // setCalcLoading(false);
    }
  }, [coworkingId]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setCartHydrated(false);

    loadInitData(null, false)
      .then(() => {
        if (!active) return;
        setCart(readStoredCart(coworkingId));
        setCartHydrated(true);
        setErrorMessage(null);
      })
      .catch((error: unknown) => {
        if (!active) return;
        setErrorMessage(error instanceof ClientRequestError ? error.message : 'Не удалось загрузить страницу бронирования.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [coworkingId, loadInitData]);

  useEffect(() => {
    if (!cartHydrated || !selectedDate || !initData || initData.previewDate === selectedDate) return;

    loadInitData(selectedDate, true)
      .catch((error: unknown) => {
        setErrorMessage(error instanceof ClientRequestError ? error.message : 'Не удалось обновить доступность мест.');
      });
  }, [cartHydrated, initData, loadInitData, selectedDate]);

  useEffect(() => {
    if (!cartHydrated) return;
    window.localStorage.setItem(cartKey(coworkingId), JSON.stringify({ coworkingId, items: cart }));
  }, [cart, cartHydrated, coworkingId]);

  useEffect(() => {
    function handleStorage(event: StorageEvent): void {
      if (event.key !== cartKey(coworkingId)) return;
      setCart(readStoredCart(coworkingId));
    }

    window.addEventListener('storage', handleStorage);
    return () => {
      window.removeEventListener('storage', handleStorage);
    };
  }, [coworkingId]);

  useEffect(() => {
    if (!initData || initData.membershipStatus !== 'active' || !cartHydrated) {
      if (initData) setCalculation(emptyCalculation(coworkingId, initData.balanceMinorUnits));
      return;
    }

    void recalculateCart(cart, initData.balanceMinorUnits);
  }, [cart, cartHydrated, coworkingId, initData, recalculateCart]);

  useEffect(() => {
    if (!initData || initData.membershipStatus !== 'active') return;

    const intervalId = window.setInterval(() => {
      const storedCart = readStoredCart(coworkingId);
      setCart((current) => {
        const currentSerialized = JSON.stringify(current);
        const storedSerialized = JSON.stringify(storedCart);
        return currentSerialized === storedSerialized ? current : storedCart;
      });

      loadInitData(selectedDate || null, true)
        .then((data) => recalculateCart(storedCart, data.balanceMinorUnits))
        .catch(() => {
          // keep the latest successful state on periodic refresh failures
        });
    }, 5000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [coworkingId, initData, loadInitData, recalculateCart, selectedDate]);

  const availableTypes = useMemo(() => Array.from(new Set((initData?.places ?? []).map((place) => place.placeTypeName))), [initData]);
  const availableFloors = useMemo(() => Array.from(new Set((initData?.places ?? []).map((place) => place.floorName))), [initData]);

  const filteredPlaces = useMemo(() => {
    return (initData?.places ?? []).filter((place) => {
      const byAvailability = place.previewAvailable;
      const byType = typeFilter === 'all' || place.placeTypeName === typeFilter;
      const byFloor = floorFilter === 'all' || place.floorName === floorFilter;
      const byPrice = !priceMax || place.pricePerDay <= Number(priceMax) * 100;
      return byAvailability && byType && byFloor && byPrice;
    });
  }, [floorFilter, initData, priceMax, typeFilter]);

  const displayedCartItems = useMemo(() => {
    if (!initData) return calculation.items;

    const calculatedByKey = new Map(calculation.items.map((item) => [`${item.placeId}-${item.date}`, item]));

    return cart.map((item) => {
      const key = `${item.placeId}-${item.date}`;
      const calculatedItem = calculatedByKey.get(key);
      if (calculatedItem) return calculatedItem;

      const place = initData.places.find((candidate) => candidate.id === item.placeId);
      const basePrice = place?.pricePerDay ?? 0;
      const fallback: CartCalculatedItem = {
        placeId: item.placeId,
        placeName: place?.name ?? `Место #${item.placeId}`,
        date: item.date,
        floor: place?.floorName ?? 'Этаж не указан',
        typeName: place?.placeTypeName ?? 'Тип не указан',
        tariffId: place?.tariffId ?? 0,
        basePrice,
        discountPercent: 0,
        discountAmount: 0,
        finalPrice: basePrice,
        available: true,
      };
      return fallback;
    });
  }, [calculation.items, cart, initData]);

  const canCheckout = Boolean(
    initData &&
    initData.membershipStatus === 'active' &&
    cart.length > 0 &&
    calculation.summary.canCheckout &&
    !calcLoading &&
    !submitting
  );

  function addPlace(place: BookingInitPlace): void {
    if (!selectedDate) return;
    setCart((current) => normalizeCart([...current, { placeId: place.id, date: selectedDate }]));
  }

  function removeItem(item: BookingCartItem): void {
    setCart((current) => current.filter((candidate) => !(candidate.placeId === item.placeId && candidate.date === item.date)));
  }

  function clearCart(): void {
    setCart([]);
  }

  async function checkout(): Promise<void> {
    if (!canCheckout) return;
    setSubmitting(true);
    try {
      await requestJson('/api/bookings/create-from-cart', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coworkingId, items: cart }),
      });
      window.localStorage.removeItem(cartKey(coworkingId));
      notifyCoworkingContextChanged();
      router.push(`/coworkings/${coworkingId}/bookings`);
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof ClientRequestError ? error.message : 'Не удалось оформить бронирование.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="alert alert-light border mb-0">Загрузка страницы бронирования...</div>;
  if (errorMessage && !initData) return <div className="alert alert-danger mb-0">{errorMessage}</div>;
  if (!initData) return <div className="alert alert-warning mb-0">Коворкинг не найден.</div>;

  if (initData.membershipStatus !== 'active') {
    return (
      <div className="card border-0 shadow-sm">
        <div className="card-body p-4 d-grid gap-3">
          <h1 className="h4 mb-0">Бронирование недоступно</h1>
          <div className="text-body-secondary">
            Для оформления бронирования нужен активный доступ. Текущий
            статус: <strong>{initData.membershipStatus}</strong>.
          </div>
          <div>
            <Link href={`/coworkings/${coworkingId}`} className="btn btn-primary">
              Вернуться в коворкинг
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div>
            <h1 className="h3 mb-2">Новое бронирование</h1>
            <div className="text-body-secondary">Выберите дату, просмотрите свободные места и подтвердите
              оформление корзины.
            </div>
          </div>
        </div>
      </section>

      {errorMessage ? <div className="alert alert-danger mb-0">{errorMessage}</div> : null}

      <div className="row g-4 align-items-start">
        <div className="col-12 col-xl-3">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <h2 className="h5 mb-0">Параметры</h2>
              <div>
                <label className="form-label">Дата бронирования</label>
                <input className="form-control" type="date" value={selectedDate} min={initData.previewDate}
                       onChange={(event) => setSelectedDate(event.target.value)}/>
              </div>
              <div>
                <label className="form-label">Тип места</label>
                <select className="form-select" value={typeFilter}
                        onChange={(event) => setTypeFilter(event.target.value)}>
                  <option value="all">Все типы</option>
                  {availableTypes.map((item) => <option key={item} value={item}>{item}</option>)}
                </select>
              </div>
              <div>
                <label className="form-label">Этаж</label>
                <select className="form-select" value={floorFilter}
                        onChange={(event) => setFloorFilter(event.target.value)}>
                  <option value="all">Все этажи</option>
                  {availableFloors.map((item) => <option key={item} value={item}>{item}</option>)}
                </select>
              </div>
              <div>
                <label className="form-label">Цена до, ₽</label>
                <input className="form-control" inputMode="numeric" value={priceMax}
                       onChange={(event) => setPriceMax(event.target.value)} placeholder="Например, 3000"/>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <h2 className="h5 mb-1">Доступные места</h2>
                  <div className="text-body-secondary small">Показаны только места, свободные
                    на {selectedDate ? formatDate(selectedDate) : 'выбранную дату'}</div>
                </div>
              </div>

              {filteredPlaces.length === 0 ? (
                <div className="text-body-secondary">По выбранным параметрам сейчас нет доступных мест.</div>
              ) : filteredPlaces.map((place) => (
                <div className="card border" key={place.id}>
                  <div className="card-body">
                    <div className="d-flex justify-content-between gap-3">
                      <div>
                        <div className="fw-semibold">{place.name}</div>
                        <div className="small text-body-secondary">{place.floorName} · {place.placeTypeName}</div>
                        <div className="small mt-2">{place.amenities.join(' · ') || 'Без дополнительных опций'}</div>
                      </div>
                      <div className="text-end">
                        <div className="fw-semibold">{formatMoney(place.pricePerDay)}</div>
                      </div>
                    </div>
                    <div className="d-flex justify-content-end mt-3">
                      <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => addPlace(place)}
                              disabled={!selectedDate}>
                        Добавить
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-4">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <h2 className="h5 mb-1">Корзина</h2>
                  <div className="text-body-secondary small">Выбранные позиции для оформления</div>
                </div>
                <button type="button" className="btn btn-link btn-sm text-decoration-none p-0" onClick={clearCart}>
                  Очистить
                </button>
              </div>

              {cart.length === 0 ? (
                <div className="text-body-secondary">Корзина пуста. Добавьте места и даты бронирования.</div>
              ) : (
                <div className="d-grid gap-3">
                  {displayedCartItems.map((item) => (
                    <div key={`${item.placeId}-${item.date}`}
                         className={`card border ${item.available ? '' : 'border-danger-subtle bg-danger-subtle'}`}>
                      <div className="card-body">
                        <div className="d-flex justify-content-between gap-3 mb-2">
                          <div>
                            <div className="fw-semibold">{item.placeName}</div>
                            <div className="small text-body-secondary">{item.floor} · {item.typeName}</div>
                          </div>
                          <button type="button" className="btn-close" aria-label="Удалить"
                                  onClick={() => removeItem({ placeId: item.placeId, date: item.date })}/>
                        </div>
                        <div className="small mb-2">{formatDate(item.date)}</div>
                        <div className="d-flex justify-content-between small">
                          <span>Базовая цена</span><span>{formatMoney(item.basePrice)}</span></div>
                        <div className="d-flex justify-content-between small">
                          <span>Скидка {item.discountPercent}%</span><span>-{formatMoney(item.discountAmount)}</span>
                        </div>
                        <div className="d-flex justify-content-between fw-semibold mt-2">
                          <span>Итого</span><span>{formatMoney(item.finalPrice)}</span></div>
                        {!item.available ?
                          <div className="small text-danger mt-2">Эта позиция недоступна для оформления.</div> : null}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="border-top pt-3 d-grid gap-2">
                <div className="d-flex justify-content-between"><span
                  className="text-body-secondary">Базовая сумма</span><span>{formatMoney(calculation.summary.totalBasePrice)}</span>
                </div>
                <div className="d-flex justify-content-between"><span
                  className="text-body-secondary">Скидка</span><span>-{formatMoney(calculation.summary.totalDiscount)}</span>
                </div>
                <div className="d-flex justify-content-between fw-semibold fs-5">
                  <span>Итого</span><span>{formatMoney(calculation.summary.totalFinalPrice)}</span></div>
                <div className="d-flex justify-content-between"><span className="text-body-secondary">Баланс после списания</span><span>{formatMoney(calculation.summary.balanceAfterMinorUnits)}</span>
                </div>
                {calculation.summary.discountHints.map((hint) => <div key={hint}
                                                                      className="small text-body-secondary">{hint}</div>)}
                {calculation.summary.validationErrors.map((error) => <div key={error}
                                                                          className="alert alert-warning py-2 mb-0">{error}</div>)}
                {calculation.summary.unavailableCount > 0 ?
                  <div className="alert alert-warning py-2 mb-0">Перед подтверждением удалите недоступные
                    позиции.</div> : null}
                {!calculation.summary.hasEnoughBalance && cart.length > 0 ?
                  <div className="alert alert-danger py-2 mb-0">Недостаточно средств для оформления
                    бронирования.</div> : null}
                <div className="small text-body-secondary">Перед подтверждением система еще раз проверит стоимость и
                  доступность выбранных мест.
                </div>
                <button type="button" className="btn btn-primary btn-lg" disabled={!canCheckout} onClick={checkout}>
                  {submitting ? 'Оформляем...' : 'Подтвердить бронирование'}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
