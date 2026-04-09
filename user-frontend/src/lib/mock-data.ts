import {
  Booking,
  BookingCartItem,
  CartCalculatedItem,
  CartCalculationSummary,
  CoworkingSummary,
  LedgerEntry,
  MembershipSummary,
  PayRequest,
  Place,
  RequestMessage,
  ServiceRequest,
  ServiceRequestTypeOption,
  UserProfile,
} from '@/lib/types';

export const currentUser: UserProfile = {
  id: 1,
  name: 'Дарья',
  email: 'darya@example.com',
  description: 'Описание профиля.',
  avatarLabel: 'Д',
};

export const memberships: MembershipSummary[] = [
  {
    id: 101,
    coworkingId: 1,
    coworkingName: 'Коворкинг 1',
    status: 'active',
    scheduleLabel: 'Пн–Вс, 08:00–22:00',
    address: 'Город, улица 1',
    balance: 1845000,
  },
  {
    id: 102,
    coworkingId: 2,
    coworkingName: 'Коворкинг 2',
    status: 'pending',
    scheduleLabel: 'Пн–Сб, 09:00–21:00',
    address: 'Город, улица 2',
    balance: 0,
  },
  {
    id: 103,
    coworkingId: 3,
    coworkingName: 'Коворкинг 3',
    status: 'blocked',
    scheduleLabel: 'Пн–Пт, 10:00–20:00',
    address: 'Город, улица 3',
    balance: 42000,
  },
];

export const coworkings: CoworkingSummary[] = [
  {
    id: 1,
    name: 'Коворкинг 1',
    scheduleLabel: 'Пн–Вс, 08:00–22:00',
    address: 'Город, улица 1',
    heroTitle: 'Коворкинг 1 с рабочими местами и переговорными',
    heroText: 'Описание коворкинга 1.',
    autoApproveMembership: false,
  },
  {
    id: 2,
    name: 'Коворкинг 2',
    scheduleLabel: 'Пн–Сб, 09:00–21:00',
    address: 'Город, улица 2',
    heroTitle: 'Коворкинг 2 для индивидуальной работы',
    heroText: 'Пока membership ожидает подтверждения, пространство доступно только в read-only режиме.',
    autoApproveMembership: false,
  },
  {
    id: 3,
    name: 'Коворкинг 3',
    scheduleLabel: 'Пн–Пт, 10:00–20:00',
    address: 'Санкт-Петербург, набережная Обводного канала 54',
    heroTitle: 'Коворкинг 3 для командных встреч',
    heroText: 'Membership заблокирован: доступны только профиль, история операций, бронирований и существующие заявки.',
    autoApproveMembership: false,
  },
];

export const places: Place[] = [
  { id: 1, coworkingId: 1, name: 'Место 1', floor: 'Этаж 1', floorId: 1, typeName: 'Тип места 1', tariffId: 11, pricePerDay: 18000, amenities: ['Опция 1', 'Опция 2', 'Опция 3'], available: true },
  { id: 2, coworkingId: 1, name: 'Место 2', floor: 'Этаж 1', floorId: 1, typeName: 'Тип места 2', tariffId: 12, pricePerDay: 22000, amenities: ['Опция 1', 'Опция 2'], available: true },
  { id: 3, coworkingId: 1, name: 'Место 3', floor: 'Этаж 2', floorId: 2, typeName: 'Тип места 3', tariffId: 13, pricePerDay: 65000, amenities: ['Опция 1', 'Опция 2', 'Опция 3'], available: true },
  { id: 4, coworkingId: 2, name: 'Место 4', floor: 'Этаж 1', floorId: 1, typeName: 'Тип места 1', tariffId: 21, pricePerDay: 16000, amenities: ['Опция 1'], available: true },
  { id: 5, coworkingId: 3, name: 'Место 5', floor: 'Этаж 2', floorId: 2, typeName: 'Тип места 4', tariffId: 31, pricePerDay: 26000, amenities: ['Опция 1', 'Опция 2'], available: true },
];

export const bookings: Booking[] = [
  { id: 201, coworkingId: 1, requestId: 'REQ-APR-01', placeName: 'Место 1', date: '2026-04-23', cost: 18000, active: true, fullRefundHoursBefore: 48, lateCancellationRefundPercent: 50, cancellationPreview: 18000 },
  { id: 202, coworkingId: 1, requestId: 'REQ-APR-01', placeName: 'Место 1', date: '2026-04-24', cost: 18000, active: true, fullRefundHoursBefore: 48, lateCancellationRefundPercent: 50, cancellationPreview: 9000 },
  { id: 203, coworkingId: 1, requestId: 'REQ-MAR-04', placeName: 'Место 3', date: '2026-03-18', cost: 65000, active: false, fullRefundHoursBefore: 72, lateCancellationRefundPercent: 20 },
  { id: 204, coworkingId: 3, requestId: 'REQ-FEB-09', placeName: 'Место 5', date: '2026-02-14', cost: 26000, active: false, fullRefundHoursBefore: 24, lateCancellationRefundPercent: 0 },
];

export const ledgerEntries: LedgerEntry[] = [
  { id: 301, coworkingId: 1, timestamp: '2026-04-20T09:15:00Z', type: 'DEPOSIT', name: 'Пополнение баланса', comment: 'Подтверждено администратором', amount: 250000 },
  { id: 302, coworkingId: 1, timestamp: '2026-04-20T09:30:00Z', type: 'BOOKING_CHARGE', name: 'Оплата бронирования', comment: 'Место 1 · 23 апреля 2026', amount: -18000 },
  { id: 303, coworkingId: 1, timestamp: '2026-04-20T09:31:00Z', type: 'BOOKING_CHARGE', name: 'Оплата бронирования', comment: 'Место 1 · 24 апреля 2026', amount: -18000 },
  { id: 304, coworkingId: 1, timestamp: '2026-04-10T08:00:00Z', type: 'CANCELLATION_REFUND', name: 'Возврат за отмену', comment: 'Полный возврат по часовому правилу тарифа', amount: 65000 },
  { id: 305, coworkingId: 1, timestamp: '2026-04-02T12:00:00Z', type: 'DAY_CLOSURE_COMPENSATION', name: 'Компенсация за закрытие дня', comment: 'Переговорная была закрыта администратором', amount: 50000 },
  { id: 306, coworkingId: 3, timestamp: '2026-04-01T10:00:00Z', type: 'MEMBERSHIP_BLOCK_COMPENSATION', name: 'Компенсация при блокировке membership', comment: 'Возврат за затронутые будущие бронирования', amount: 42000 },
];

export const payRequests: PayRequest[] = [
  { id: 401, coworkingId: 1, amount: 1500000, status: 'Approved', userComment: 'Пополнение для бронирований на неделю', createdAt: '2026-04-19T09:00:00Z' },
  { id: 402, coworkingId: 1, amount: -70000, status: 'Pending', userComment: 'Вывод остатка', createdAt: '2026-04-21T07:30:00Z' },
  { id: 403, coworkingId: 3, amount: 250000, status: 'Rejected', userComment: 'Пополнение для будущих визитов', createdAt: '2026-03-10T11:15:00Z' },
];

export const serviceRequests: ServiceRequest[] = [
  { id: 501, coworkingId: 1, name: 'Заявка 1', typeName: 'Тип заявки 1', cost: 15000, status: 'in_progress', createdAt: '2026-04-20T11:00:00Z', updatedAt: '2026-04-21T08:30:00Z' },
  { id: 502, coworkingId: 1, name: 'Заявка 2', typeName: 'Тип заявки 2', cost: 6000, status: 'resolved', createdAt: '2026-04-12T10:00:00Z', updatedAt: '2026-04-12T15:00:00Z' },
  { id: 503, coworkingId: 3, name: 'Заявка 3', typeName: 'Тип заявки 3', cost: 0, status: 'new', createdAt: '2026-04-18T09:30:00Z', updatedAt: '2026-04-18T09:30:00Z' },
];

export const requestMessages: RequestMessage[] = [
  { id: 601, requestId: 501, authorType: 'USER', authorName: 'Дарья Морозова', text: 'Нужна настройка места 3 к 15:00.', timestamp: '2026-04-20T11:00:00Z', readAt: '2026-04-20T11:10:00Z' },
  { id: 602, requestId: 501, authorType: 'ADMIN', authorName: 'Иван Петров', text: 'Место 3 подготовлено, проверьте доступ за 10 минут до встречи.', timestamp: '2026-04-21T08:30:00Z' },
  { id: 603, requestId: 503, authorType: 'USER', authorName: 'Дарья Морозова', text: 'Прошу пересмотреть статус блокировки. Все прошлые обязательства закрыты.', timestamp: '2026-04-18T09:30:00Z' },
];

export const serviceRequestTypes: ServiceRequestTypeOption[] = [
  {id: 701, coworkingId: 1, name: 'Тип заявки 1', cost: 15000},
  {id: 702, coworkingId: 1, name: 'Тип заявки 2', cost: 6000},
  {id: 703, coworkingId: 1, name: 'Тип заявки 4', cost: 9000},
  {id: 704, coworkingId: 3, name: 'Тип заявки 3', cost: 0},
];

const unavailableByDate: Record<string, number[]> = {
  '2026-04-25': [3],
  '2026-04-26': [2],
};

export function getMembership(coworkingId: number): MembershipSummary | undefined {
  return memberships.find((item) => item.coworkingId === coworkingId);
}

export function getCoworking(coworkingId: number): CoworkingSummary | undefined {
  return coworkings.find((item) => item.id === coworkingId);
}

export function getPlaces(coworkingId: number): Place[] {
  return places.filter((item) => item.coworkingId === coworkingId);
}

export function getBookings(coworkingId: number): Booking[] {
  return bookings.filter((item) => item.coworkingId === coworkingId);
}

export function getLedgerEntries(coworkingId: number): LedgerEntry[] {
  return ledgerEntries.filter((item) => item.coworkingId === coworkingId);
}

export function getPayRequests(coworkingId: number): PayRequest[] {
  return payRequests.filter((item) => item.coworkingId === coworkingId);
}

export function getServiceRequests(coworkingId: number): ServiceRequest[] {
  return serviceRequests.filter((item) => item.coworkingId === coworkingId);
}

export function getServiceRequest(coworkingId: number, requestId: number): ServiceRequest | undefined {
  return serviceRequests.find((item) => item.coworkingId === coworkingId && item.id === requestId);
}

export function getRequestMessages(requestId: number): RequestMessage[] {
  return requestMessages.filter((item) => item.requestId === requestId);
}

export function getServiceRequestTypes(coworkingId: number): ServiceRequestTypeOption[] {
  return serviceRequestTypes.filter((item) => item.coworkingId === coworkingId);
}

function getDiscountPercentForTariff(tariffId: number, quantity: number): number {
  const rulesByTariff: Record<number, {threshold: number; percent: number}[]> = {
    11: [{threshold: 3, percent: 10}, {threshold: 5, percent: 15}],
    12: [{threshold: 2, percent: 8}],
    13: [{threshold: 2, percent: 5}],
    21: [{threshold: 4, percent: 10}],
    31: [{threshold: 3, percent: 12}],
  };

  return (rulesByTariff[tariffId] ?? [])
    .filter((rule) => quantity >= rule.threshold)
    .reduce((max, rule) => Math.max(max, rule.percent), 0);
}

function isItemAvailable(placeId: number, date: string): boolean {
  return !(unavailableByDate[date] ?? []).includes(placeId);
}

export function calculateCart(coworkingId: number, items: BookingCartItem[]): {
  items: CartCalculatedItem[];
  summary: CartCalculationSummary;
} {
  const coworkingPlaces = getPlaces(coworkingId);
  const quantitiesByTariff = new Map<number, number>();

  items.forEach((item) => {
    const place = coworkingPlaces.find((candidate) => candidate.id === item.placeId);
    if (!place) {
      return;
    }
    quantitiesByTariff.set(place.tariffId, (quantitiesByTariff.get(place.tariffId) ?? 0) + 1);
  });

  const calculatedItems = items.map((item) => {
    const place = coworkingPlaces.find((candidate) => candidate.id === item.placeId);
    if (!place) {
      return {
        placeId: item.placeId,
        placeName: 'Неизвестное место',
        date: item.date,
        floor: '—',
        typeName: '—',
        basePrice: 0,
        discountPercent: 0,
        discountAmount: 0,
        finalPrice: 0,
        available: false,
      };
    }

    const discountPercent = getDiscountPercentForTariff(place.tariffId, quantitiesByTariff.get(place.tariffId) ?? 0);
    const basePrice = place.pricePerDay;
    const discountAmount = Math.floor((basePrice * discountPercent) / 100);
    const finalPrice = basePrice - discountAmount;

    return {
      placeId: place.id,
      placeName: place.name,
      date: item.date,
      floor: place.floor,
      typeName: place.typeName,
      basePrice,
      discountPercent,
      discountAmount,
      finalPrice,
      available: place.available && isItemAvailable(place.id, item.date),
    };
  });

  const totalBasePrice = calculatedItems.reduce((sum, item) => sum + item.basePrice, 0);
  const totalDiscount = calculatedItems.reduce((sum, item) => sum + item.discountAmount, 0);
  const totalFinalPrice = calculatedItems.reduce((sum, item) => sum + item.finalPrice, 0);
  const unavailableCount = calculatedItems.filter((item) => !item.available).length;
  const discountHints = Array.from(quantitiesByTariff.entries()).flatMap(([tariffId, quantity]) => {
    const rules = ({
      11: [{threshold: 3, percent: 10}, {threshold: 5, percent: 15}],
      12: [{threshold: 2, percent: 8}],
      13: [{threshold: 2, percent: 5}],
      21: [{threshold: 4, percent: 10}],
      31: [{threshold: 3, percent: 12}],
    } as Record<number, {threshold: number; percent: number}[]>)[tariffId] ?? [];
    const nextRule = rules.find((rule) => quantity < rule.threshold);
    return nextRule ? [`Добавьте ещё ${nextRule.threshold - quantity} поз. этого тарифа для скидки ${nextRule.percent}%`] : [];
  });

  return {
    items: calculatedItems,
    summary: {
      totalBasePrice,
      totalDiscount,
      totalFinalPrice,
      unavailableCount,
      discountHints,
    },
  };
}
