'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { formatDate, formatMoney } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type {
  BookingCartItem,
  BookingInitData,
  BookingInitFloor,
  BookingInitPlace,
  CartCalculatedItem,
  CartCalculation,
  PlaceAvailabilityDay,
} from '@/lib/types';
import { notifyCoworkingContextChanged } from '@/components/layout/coworking-shell-context';
import { ImagePreviewModal } from '@/components/ui/image-preview-modal';
import { useToast } from '@/components/ui/toast-provider';
import { LoadingOverlay } from '@/components/ui/loading-overlay';

function cartKey(membershipId: number): string {
  return `booking_cart_${membershipId}`;
}

function normalizeCart(items: BookingCartItem[]): BookingCartItem[] {
  const unique = new Map<string, BookingCartItem>();
  for (const item of items) unique.set(`${item.placeId}-${item.date}`, item);
  return Array.from(unique.values());
}

function readStoredCart(membershipId: number): BookingCartItem[] {
  try {
    const raw = window.localStorage.getItem(cartKey(membershipId));
    if (!raw) return [];
    const parsed = JSON.parse(raw) as { membershipId?: number; items?: BookingCartItem[] };
    return parsed.membershipId === membershipId && Array.isArray(parsed.items) ? normalizeCart(parsed.items) : [];
  } catch {
    return [];
  }
}

function emptyCalculation(_membershipId: number, balance = 0): CartCalculation {
  return {
    items: [],
    summary: {
      totalFinalPrice: 0,
      unavailableCount: 0,
      validationErrors: [],
      hasEnoughBalance: true,
      balanceAfterMinorUnits: balance,
      canCheckout: false,
    },
  };
}

type PlaceUiState = 'available' | 'unavailable' | 'inCart';

type FloorMapViewProps = {
  floor: BookingInitFloor | null;
  places: BookingInitPlace[];
  selectedDate: string;
  selectedPlaceId: number | null;
  cart: BookingCartItem[];
  onSelectPlace: (place: BookingInitPlace) => void;
};

function isCartSelected(cart: BookingCartItem[], placeId: number, date: string): boolean {
  return cart.some((item) => item.placeId === placeId && item.date === date);
}

function hasMapCoordinates(place: BookingInitPlace): boolean {
  return typeof place.locX === 'number' && typeof place.locY === 'number';
}

function normalizePercentCoordinate(value: number): number {
  if (value >= 0 && value <= 1) return value * 100;
  return Math.min(Math.max(value, 0), 100);
}

function getPlaceState(place: BookingInitPlace, selectedDate: string, cart: BookingCartItem[]): PlaceUiState {
  if (selectedDate && isCartSelected(cart, place.id, selectedDate)) return 'inCart';
  return place.available ? 'available' : 'unavailable';
}

function placeStateLabel(state: PlaceUiState): string {
  if (state === 'inCart') return 'Уже в корзине';
  if (state === 'available') return 'Свободно';
  return 'Недоступно';
}

function dateDayNumber(date: string): number {
  return new Date(`${date}T00:00:00`).getDate();
}

const WEEKDAY_LABELS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

type AvailabilityMonth = {
  key: string;
  title: string;
  leadingEmptyCells: number;
  days: PlaceAvailabilityDay[];
};

function monthLabel(date: Date): string {
  return new Intl.DateTimeFormat('ru-RU', { month: 'long', year: 'numeric' }).format(date);
}

function toLocalDate(date: string): Date {
  return new Date(`${date}T00:00:00`);
}

function buildAvailabilityMonths(days: PlaceAvailabilityDay[]): AvailabilityMonth[] {
  const grouped = new Map<string, PlaceAvailabilityDay[]>();
  for (const day of days) {
    const date = toLocalDate(day.date);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    grouped.set(key, [...(grouped.get(key) ?? []), day]);
  }

  return Array.from(grouped.entries()).map(([key, monthDays]) => {
    const first = toLocalDate(monthDays[0].date);
    const mondayBasedDay = (first.getDay() + 6) % 7;
    return {
      key,
      title: monthLabel(first),
      leadingEmptyCells: mondayBasedDay,
      days: monthDays,
    };
  });
}

function FloorMapView({ floor, places, selectedDate, selectedPlaceId, cart, onSelectPlace }: FloorMapViewProps) {
  const placesWithCoordinates = places.filter(hasMapCoordinates);

  if (!floor) {
    return (
      <div className="border rounded-4 bg-body p-3 mb-0">
        В коворкинге пока нет этажей для отображения карты.
      </div>
    );
  }

  return (
    <div className="floor-map rounded-4 border overflow-hidden position-relative bg-body-tertiary">
      {(floor.imageUrl) ? (
        <>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={floor.imageUrl!} alt={`План этажа ${floor.name}`} className="floor-map__image"/>
        </>
      ) : (
        <div
          className="floor-map__placeholder d-flex align-items-center justify-content-center text-body-secondary text-center p-4">
          План этажа пока не загружен администратором
        </div>
      )}

      {placesWithCoordinates.map((place) => {
        const state = getPlaceState(place, selectedDate, cart);
        const selected = selectedPlaceId === place.id;
        return (
          <button
            key={place.id}
            type="button"
            className={`floor-map__marker floor-map__marker--${state === 'unavailable' ? 'busy' : 'available'} ${state === 'inCart' ? 'floor-map__marker--in-cart' : ''} ${selected ? 'floor-map__marker--selected' : ''}`}
            style={{
              left: `${normalizePercentCoordinate(place.locX ?? 0)}%`,
              top: `${normalizePercentCoordinate(place.locY ?? 0)}%`,
            }}
            title={`${place.name} · ${place.placeTypeName} · ${placeStateLabel(state)}`}
            onClick={() => onSelectPlace(place)}
          >
            <span className="visually-hidden">{place.name}</span>
            <span aria-hidden="true">{place.name}</span>
          </button>
        );
      })}
    </div>
  );
}

export function BookingNewPage({ membershipId, initialData, initialError = null }: {
  membershipId: number;
  initialData: BookingInitData | null;
  initialError?: string | null
}) {
  const router = useRouter();
  const toast = useToast();
  const selectedPlaceCardRef = useRef<HTMLDivElement | null>(null);
  const mapSectionRef = useRef<HTMLDivElement | null>(null);
  const [initData, setInitData] = useState<BookingInitData | null>(initialData);
  const [cart, setCart] = useState<BookingCartItem[]>([]);
  const [calculation, setCalculation] = useState<CartCalculation>(emptyCalculation(membershipId, initialData?.balanceMinorUnits ?? 0));
  const [selectedDate, setSelectedDate] = useState(initialData?.previewDate ?? '');
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const [selectedFloorId, setSelectedFloorId] = useState<number | null>(null);
  const [typeFilter, setTypeFilter] = useState('all');
  const [floorFilter, setFloorFilter] = useState('all');
  const [priceMax, setPriceMax] = useState('');
  const [selectedAvailability, setSelectedAvailability] = useState<PlaceAvailabilityDay[]>([]);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [calcLoading, setCalcLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [cartHydrated, setCartHydrated] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(initialError);
  const [previewImage, setPreviewImage] = useState<{ url: string; title: string } | null>(null);

  const loadInitData = useCallback(async (date: string | null, preserveDate: boolean): Promise<BookingInitData> => {
    const query = date ? `?date=${encodeURIComponent(date)}` : '';
    const data = await requestJson<BookingInitData>(`/api/memberships/${membershipId}/booking/init${query}`);
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
      return emptyCalculation(membershipId, data.balanceMinorUnits);
    });
    if (!preserveDate || !date) setSelectedDate(data.previewDate);
    return data;
  }, [membershipId]);

  const recalculateCart = useCallback(async (items: BookingCartItem[], balanceMinorUnits: number): Promise<void> => {
    if (!items.length) {
      setCalculation(emptyCalculation(membershipId, balanceMinorUnits));
      return;
    }

    setCalcLoading(true);
    try {
      const data = await requestJson<CartCalculation>(`/api/memberships/${membershipId}/bookings/cart/calculate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items }),
      });
      setCalculation(data);
      setErrorMessage(null);
    } catch (error: unknown) {
      toast.error(error instanceof ClientRequestError ? error.message : 'Не удалось пересчитать состав бронирования.');
    } finally {
      setCalcLoading(false);
    }
  }, [membershipId, toast]);

  useEffect(() => {
    setCart(readStoredCart(membershipId));
    setCartHydrated(true);
  }, [membershipId]);

  useEffect(() => {
    if (!cartHydrated || !selectedDate || !initData || initData.previewDate === selectedDate) return;

    loadInitData(selectedDate, true)
      .catch((error: unknown) => {
        toast.error(error instanceof ClientRequestError ? error.message : 'Не удалось обновить доступность мест.');
      });
  }, [cartHydrated, initData, loadInitData, selectedDate, toast]);

  useEffect(() => {
    if (!cartHydrated) return;
    window.localStorage.setItem(cartKey(membershipId), JSON.stringify({ membershipId, items: cart }));
  }, [cart, cartHydrated, membershipId]);

  useEffect(() => {
    function handleStorage(event: StorageEvent): void {
      if (event.key !== cartKey(membershipId)) return;
      setCart(readStoredCart(membershipId));
    }

    window.addEventListener('storage', handleStorage);
    return () => {
      window.removeEventListener('storage', handleStorage);
    };
  }, [membershipId]);

  useEffect(() => {
    if (!initData || initData.membershipStatus !== 'active' || !cartHydrated) {
      if (initData) setCalculation(emptyCalculation(membershipId, initData.balanceMinorUnits));
      return;
    }

    void recalculateCart(cart, initData.balanceMinorUnits);
  }, [cart, cartHydrated, membershipId, initData, recalculateCart]);

  useEffect(() => {
    if (!initData || initData.membershipStatus !== 'active') return;

    const intervalId = window.setInterval(() => {
      const storedCart = readStoredCart(membershipId);
      setCart((current) => JSON.stringify(current) === JSON.stringify(storedCart) ? current : storedCart);

      loadInitData(selectedDate || null, true)
        .then((data) => recalculateCart(storedCart, data.balanceMinorUnits))
        .catch(() => {
        });
    }, 5000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [membershipId, initData, loadInitData, recalculateCart, selectedDate]);

  const selectedPlace = useMemo(
    () => (initData?.places ?? []).find((place) => place.id === selectedPlaceId) ?? null,
    [initData, selectedPlaceId]
  );


  useEffect(() => {
    if (!selectedPlace?.floorId) return;
    setSelectedFloorId(selectedPlace.floorId);
  }, [selectedPlace?.floorId]);

  useEffect(() => {
    if (!selectedPlaceId) {
      setSelectedAvailability([]);
      return;
    }

    let active = true;
    setAvailabilityLoading(true);
    requestJson<PlaceAvailabilityDay[]>(`/api/memberships/${membershipId}/places/${selectedPlaceId}/availability`)
      .then((data) => {
        if (active) setSelectedAvailability(data);
      })
      .catch(() => {
        if (active) setSelectedAvailability([]);
      })
      .finally(() => {
        if (active) setAvailabilityLoading(false);
      });

    return () => {
      active = false;
    };
  }, [membershipId, selectedPlaceId]);

  const availableTypes = useMemo(() => Array.from(new Set((initData?.places ?? []).map((place) => place.placeTypeName))), [initData]);
  const availableFloors = useMemo(() => Array.from(new Set((initData?.places ?? []).map((place) => place.floorName))), [initData]);

  const hasMultipleFloors = useMemo(
    () => (initData?.floors ?? []).filter((floor) => floor.active).length > 1,
    [initData]
  );
  const mapFloors = useMemo(() => {
    if (!initData?.floorMapEnabled) return [];
    return initData.floors.filter((floor) => floor.active);
  }, [initData]);

  const selectedMapFloor = useMemo(() => {
    if (!initData?.floorMapEnabled) return null;
    const selected = selectedFloorId == null ? null : mapFloors.find((floor) => floor.id === selectedFloorId) ?? null;
    return selected ?? mapFloors[0] ?? null;
  }, [initData?.floorMapEnabled, mapFloors, selectedFloorId]);

  const mapPlaces = useMemo(() => {
    if (!selectedMapFloor) return [];
    return (initData?.places ?? []).filter((place) => place.floorId === selectedMapFloor.id);
  }, [initData, selectedMapFloor]);

  const filteredPlaces = useMemo(() => {
    return (initData?.places ?? []).filter((place) => {
      const byAvailability = place.available;
      const byType = typeFilter === 'all' || place.placeTypeName === typeFilter;
      const byFloor = floorFilter === 'all' || place.floorName === floorFilter;
      const byPrice = !priceMax || place.pricePerDay <= Number(priceMax) * 100;
      return byAvailability && byType && byFloor && byPrice;
    });
  }, [floorFilter, initData, priceMax, typeFilter]);

  const selectedPlaceState = useMemo(
    () => selectedPlace ? getPlaceState(selectedPlace, selectedDate, cart) : 'unavailable',
    [cart, selectedDate, selectedPlace]
  );

  const selectedPlaceInFilteredList = useMemo(
    () => selectedPlaceId != null && filteredPlaces.some((place) => place.id === selectedPlaceId),
    [filteredPlaces, selectedPlaceId]
  );

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
        placeName: place?.name ?? 'Место не найдено',
        date: item.date,
        floor: place?.floorName ?? 'Этаж не указан',
        typeName: place?.placeTypeName ?? 'Тип не указан',
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

  function selectPlace(place: BookingInitPlace, scrollToCard = true): void {
    setSelectedPlaceId(place.id);
    if (place.floorId) setSelectedFloorId(place.floorId);
    if (scrollToCard) {
      window.setTimeout(() => selectedPlaceCardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 0);
    }
  }

  function showSelectedPlaceOnMap(): void {
    if (selectedPlace?.floorId) setSelectedFloorId(selectedPlace.floorId);
    window.setTimeout(() => mapSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 0);
  }

  function addSelectedPlace(): void {
    if (!selectedDate || !selectedPlace || selectedPlaceState !== 'available') return;
    setCart((current) => normalizeCart([...current, { placeId: selectedPlace.id, date: selectedDate }]));
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
      await requestJson(`/api/memberships/${membershipId}/bookings/create-from-cart`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items: cart }),
      });
      window.localStorage.removeItem(cartKey(membershipId));
      notifyCoworkingContextChanged();
      toast.success('Бронирование оформлено.');
      router.push(`/memberships/${membershipId}/bookings`);
      router.refresh();
    } catch (error) {
      toast.error(error instanceof ClientRequestError ? error.message : 'Не удалось оформить бронирование.');
    } finally {
      setSubmitting(false);
    }
  }

  if (errorMessage && !initData) return <div
    className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 mb-0">{errorMessage}</div>;
  if (!initData) return <div
    className="border rounded-4 border-warning-subtle bg-warning-subtle text-warning-emphasis p-3 mb-0">Коворкинг не
    найден.</div>;

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
            <Link href={`/memberships/${membershipId}`} className="btn btn-primary">
              Вернуться в коворкинг
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const selectedCanBeAdded = selectedPlaceState === 'available';

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div>
            <h1 className="h3 mb-2">Новое бронирование</h1>
            <div className="text-body-secondary">
              Выберите место на карте или в списке, посмотрите его календарь и добавьте свободную дату в корзину.
            </div>
          </div>
        </div>
      </section>


      <div className="row g-4 align-items-start">
        <div className="col-12 col-xl-8 d-grid gap-4">
          <section className="card border-0 shadow-sm" ref={selectedPlaceCardRef}>
            <div className="card-body p-4">
              <div className="d-flex flex-column flex-lg-row gap-4">
                {(selectedPlace?.previewImageUrl ?? selectedPlace?.imageUrl) ? (
                  <div className="booking-place-visual rounded-4 border overflow-hidden bg-body-tertiary flex-shrink-0">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <button type="button" className="btn p-0 border-0 bg-transparent d-block w-100 h-100"
                            onClick={() => setPreviewImage({
                              url: selectedPlace.fullImageUrl ?? selectedPlace.imageUrl!,
                              title: selectedPlace.name
                            })}>
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={selectedPlace.previewImageUrl ?? selectedPlace.imageUrl!} alt={selectedPlace.name}
                           className="img-fluid w-100 h-100 object-fit-cover"/>
                    </button>
                  </div>
                ) : null}
                <div className="flex-grow-1 d-grid gap-3">
                  <div className="d-flex justify-content-between align-items-start gap-3">
                    <div>
                      <div className="text-uppercase text-body-secondary small fw-semibold mb-1">Выбранное место</div>
                      <h2 className="h4 mb-1">{selectedPlace?.name ?? 'Выберите место'}</h2>
                      {selectedPlace ? (
                        <div className="text-body-secondary">
                          {selectedPlace.floorName} · {selectedPlace.placeTypeName}
                        </div>
                      ) : null}
                    </div>
                    {selectedPlace ? (
                      <span
                        className={`badge rounded-pill ${selectedPlaceState === 'available' ? 'text-bg-success' : selectedPlaceState === 'inCart' ? 'text-bg-warning' : 'text-bg-secondary'}`}>
                        {placeStateLabel(selectedPlaceState)}
                      </span>
                    ) : null}
                  </div>

                  {selectedPlace ? (
                    <>
                      <div className="row g-3">
                        <div className="col-6 col-md-4">
                          <div className="text-body-secondary small">Дата</div>
                          <div className="fw-semibold">{selectedDate ? formatDate(selectedDate) : '—'}</div>
                        </div>
                        <div className="col-6 col-md-4">
                          <div className="text-body-secondary small">Цена за день</div>
                          <div className="fw-semibold">{formatMoney(selectedPlace.pricePerDay)}</div>
                        </div>
                        <div className="col-6 col-md-4">
                          <div className="text-body-secondary small">Тип</div>
                          <div className="fw-semibold">{selectedPlace.placeTypeName}</div>
                        </div>
                      </div>

                      <div>
                        <div className="text-body-secondary small mb-1">Удобства</div>
                        <div>{selectedPlace.amenities.length > 0 ? selectedPlace.amenities.join(' · ') : 'Без дополнительных опций'}</div>
                      </div>

                      <div className="d-flex flex-wrap gap-2">
                        <button type="button" className="btn btn-primary" disabled={!selectedCanBeAdded}
                                onClick={addSelectedPlace}>
                          {selectedPlaceState === 'inCart' ? 'Уже в корзине' : selectedPlaceState === 'available' ? 'Добавить в корзину' : 'Недоступно'}
                        </button>
                        {initData.floorMapEnabled && selectedPlace.floorId && hasMapCoordinates(selectedPlace) ? (
                          <button type="button" className="btn btn-outline-secondary" onClick={showSelectedPlaceOnMap}>
                            Показать на плане
                          </button>
                        ) : null}
                      </div>
                    </>
                  ) : (
                    <div className="border rounded-4 bg-body p-3 mb-0">Выберите место на карте или в списке доступных
                      мест.</div>
                  )}
                </div>
              </div>
            </div>
          </section>


          {initData.floorMapEnabled ? (
            <section className="card border-0 shadow-sm" ref={mapSectionRef}>
              <div className="card-body p-4 d-grid gap-3">
                <div>
                  <h2 className="h5 mb-1">Карта этажа</h2>
                  <div className="text-body-secondary small">
                    На карте видны свободные и недоступные места. Любое место можно выбрать, чтобы посмотреть его
                    календарь.
                  </div>
                </div>

                {mapFloors.length > 0 ? (
                  <div className="d-flex flex-wrap gap-2">
                    {mapFloors.map((floor) => (
                      <button
                        key={floor.id}
                        type="button"
                        className={`btn btn-sm ${selectedMapFloor?.id === floor.id ? 'btn-primary' : 'btn-outline-primary'}`}
                        onClick={() => setSelectedFloorId(floor.id)}
                      >
                        {floor.name}
                      </button>
                    ))}
                  </div>
                ) : null}

                <FloorMapView
                  floor={selectedMapFloor}
                  places={mapPlaces}
                  selectedDate={selectedDate}
                  selectedPlaceId={selectedPlaceId}
                  cart={cart}
                  onSelectPlace={(place) => selectPlace(place, true)}
                />

                <div className="d-flex flex-wrap gap-3 small text-body-secondary">
                  <span><span className="floor-map-legend floor-map-legend--available"/> Свободно</span>
                  <span><span className="floor-map-legend floor-map-legend--busy"/> Недоступно</span>
                  <span><span className="floor-map-legend floor-map-legend--selected"/> Выбрано</span>
                  <span><span className="floor-map-legend floor-map-legend--in-cart"/> В корзине</span>
                </div>
              </div>
            </section>
          ) : null}

          <section className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
                <div>
                  <h2 className="h5 mb-1">Свободные места</h2>
                  <div className="text-body-secondary small">
                    В списке показаны только места, доступные
                    на {selectedDate ? formatDate(selectedDate) : 'выбранную дату'}.
                  </div>
                </div>
                <div className="d-flex flex-wrap gap-2 booking-compact-filters">
                  <select className="form-select form-select-sm" value={typeFilter}
                          onChange={(event) => setTypeFilter(event.target.value)} aria-label="Фильтр по типу места">
                    <option value="all">Все типы</option>
                    {availableTypes.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                  {hasMultipleFloors ? (
                    <select className="form-select form-select-sm" value={floorFilter}
                            onChange={(event) => setFloorFilter(event.target.value)} aria-label="Фильтр по этажу">
                      <option value="all">Все этажи</option>
                      {availableFloors.map((item) => <option key={item} value={item}>{item}</option>)}
                    </select>
                  ) : null}
                  <input
                    className="form-control form-control-sm"
                    inputMode="numeric"
                    value={priceMax}
                    onChange={(event) => setPriceMax(event.target.value)}
                    placeholder="Цена до, ₽"
                    aria-label="Цена до"
                  />
                </div>
              </div>

              {selectedPlace && !selectedPlaceInFilteredList ? (
                <div className="border rounded-4 bg-body p-3 mb-0 small">
                  Выбранное место сейчас не отображается в списке, потому что список содержит только свободные места с
                  учетом фильтров.
                </div>
              ) : null}

              {filteredPlaces.length === 0 ? (
                <div className="text-body-secondary">На выбранную дату свободных мест по текущим фильтрам нет.</div>
              ) : (
                <div className="booking-place-card-grid">
                  {filteredPlaces.map((place) => {
                    const inCart = selectedDate ? isCartSelected(cart, place.id, selectedDate) : false;
                    return (
                      <button
                        key={place.id}
                        type="button"
                        className={`booking-place-card text-start ${selectedPlaceId === place.id ? 'booking-place-card--selected' : ''}`}
                        onClick={() => selectPlace(place, false)}
                      >
                        {(place.previewImageUrl ?? place.imageUrl) ? (
                          <span className="booking-place-card__image-wrap">
                            <span
                              role="button"
                              tabIndex={0}
                              className="d-block w-100 h-100"
                              onClick={(event) => {
                                event.stopPropagation();
                                setPreviewImage({ url: place.fullImageUrl ?? place.imageUrl!, title: place.name });
                              }}
                              onKeyDown={(event) => {
                                if (event.key === 'Enter' || event.key === ' ') {
                                  event.preventDefault();
                                  event.stopPropagation();
                                  setPreviewImage({ url: place.fullImageUrl ?? place.imageUrl!, title: place.name });
                                }
                              }}
                            >
                              {/* eslint-disable-next-line @next/next/no-img-element */}
                              <img src={place.previewImageUrl ?? place.imageUrl!} alt={place.name}
                                   className="img-fluid w-100 h-100 object-fit-cover"/>
                            </span>
                          </span>
                        ) : null}
                        <span className="booking-place-card__body">
                          <span className="d-flex justify-content-between align-items-start gap-2">
                            <span>
                              <span className="fw-semibold d-block">{place.name}</span>
                              <span
                                className="small text-body-secondary d-block">{place.floorName} · {place.placeTypeName}</span>
                            </span>
                            <span className={`badge ${inCart ? 'text-bg-warning' : 'text-bg-success'}`}>
                              {inCart ? 'В корзине' : 'Свободно'}
                            </span>
                          </span>
                          <span className="small text-body-secondary d-block mt-2">
                            {place.amenities.join(' · ') || 'Без дополнительных опций'}
                          </span>
                          <span className="fw-semibold d-block mt-3">{formatMoney(place.pricePerDay)}</span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </section>
        </div>

        <div className="col-12 col-xl-4 d-grid gap-4">
          <section className="card border-0 shadow-sm">
            <div className="card-body p-4 d-grid gap-3">
              <div>
                <h2 className="h5 mb-1">Календарь места</h2>
                <div className="text-body-secondary small">
                  Календарь управляет датой для всей страницы. Недоступные даты можно выбрать, но добавить место в
                  корзину получится только на свободную дату.
                </div>
              </div>

              {!selectedPlace ? (
                <div className="border rounded-4 bg-body p-3 mb-0">Выберите место, чтобы увидеть календарь
                  доступности.</div>
              ) : availabilityLoading ? (
                <LoadingOverlay message="Загрузка данных..." ariaLabel="Загрузка данных"/>
              ) : selectedAvailability.length === 0 ? (
                <div className="text-body-secondary">Календарь для выбранного места временно недоступен.</div>
              ) : (
                <div className="booking-calendar d-grid gap-3">
                  {buildAvailabilityMonths(selectedAvailability).map((month) => (
                    <div key={month.key} className="booking-calendar__month">
                      <div className="booking-calendar__title text-capitalize fw-semibold mb-2">{month.title}</div>
                      <div className="booking-calendar__weekdays">
                        {WEEKDAY_LABELS.map((label) => <div key={label}>{label}</div>)}
                      </div>
                      <div className="booking-calendar__grid">
                        {Array.from({ length: month.leadingEmptyCells }).map((_, index) => (
                          <span key={`empty-${month.key}-${index}`} className="booking-calendar__empty"
                                aria-hidden="true"/>
                        ))}
                        {month.days.map((day) => {
                          const inCart = selectedPlace ? isCartSelected(cart, selectedPlace.id, day.date) : false;
                          const state: PlaceUiState = inCart ? 'inCart' : day.available ? 'available' : 'unavailable';
                          return (
                            <button
                              key={day.date}
                              type="button"
                              className={`booking-calendar__day ${state === 'available' ? 'booking-calendar__day--available' : ''} ${state === 'unavailable' ? 'booking-calendar__day--busy' : ''} ${state === 'inCart' ? 'booking-calendar__day--in-cart' : ''} ${selectedDate === day.date ? 'booking-calendar__day--selected' : ''}`}
                              onClick={() => setSelectedDate(day.date)}
                              title={`${formatDate(day.date)} · ${placeStateLabel(state)}`}
                            >
                              {dateDayNumber(day.date)}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="d-flex flex-wrap gap-3 small text-body-secondary">
                <span><span className="floor-map-legend floor-map-legend--available"/> Свободно</span>
                <span><span className="floor-map-legend floor-map-legend--busy"/> Недоступно</span>
                <span><span className="floor-map-legend floor-map-legend--in-cart"/> В корзине</span>
              </div>
            </div>
          </section>

          <div className="card border-0 shadow-sm booking-cart-card">
            <div className="card-body p-4 d-grid gap-3">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <h2 className="h5 mb-1">Корзина</h2>
                  <div className="text-body-secondary small">Выбранные позиции для оформления</div>
                </div>
                <button type="button" className="btn btn-link btn-sm text-decoration-none p-0" onClick={clearCart}
                        disabled={cart.length === 0}>
                  Очистить
                </button>
              </div>

              {cart.length === 0 ? (
                <div className="text-body-secondary">Корзина пуста. Выберите место и свободную дату.</div>
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
                        <div className="d-flex justify-content-between fw-semibold mt-2">
                          <span>Цена</span><span>{formatMoney(item.finalPrice)}</span></div>
                        {!item.available ?
                          <div className="small text-danger mt-2">Эта позиция недоступна для оформления.</div> : null}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="border-top pt-3 d-grid gap-2">
                <div className="d-flex justify-content-between fw-semibold fs-5">
                  <span>Итого</span><span>{formatMoney(calculation.summary.totalFinalPrice)}</span></div>
                <div className="d-flex justify-content-between"><span className="text-body-secondary">Баланс после списания</span><span>{formatMoney(calculation.summary.balanceAfterMinorUnits)}</span>
                </div>
                {calculation.summary.validationErrors.map((error) => <div key={error}
                                                                          className="border rounded-4 border-warning-subtle bg-warning-subtle text-warning-emphasis p-3 py-2 mb-0">{error}</div>)}
                {calculation.summary.unavailableCount > 0 ? <div
                  className="border rounded-4 border-warning-subtle bg-warning-subtle text-warning-emphasis p-3 py-2 mb-0">Перед
                  подтверждением удалите недоступные позиции.</div> : null}
                {!calculation.summary.hasEnoughBalance && cart.length > 0 ? <div
                  className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 py-2 mb-0">Недостаточно
                  средств для оформления бронирования.</div> : null}
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
      <ImagePreviewModal image={previewImage} onClose={() => setPreviewImage(null)}/>
    </div>
  );
}
