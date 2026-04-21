'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { formatDate, formatMoney } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { Booking, BookingInitData, MembershipStatus, UserCoworkingDetails } from '@/lib/types';

type QuickAction = {
  label: string;
  href: string;
  disabled: boolean;
  variant: 'primary' | 'outline-primary' | 'outline-secondary';
};

type StatusContent = {
  title: string;
  description: string;
};

export function CoworkingPageContent({ coworkingId }: { coworkingId: number }) {
  const [coworking, setCoworking] = useState<UserCoworkingDetails | null>(null);
  const [bookingInit, setBookingInit] = useState<BookingInitData | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadPageData() {
      try {
        const [coworkingData, bookingInitData, bookingItems] = await Promise.all([
          requestJson<UserCoworkingDetails>(`/api/coworkings/${coworkingId}`),
          requestJson<BookingInitData>(`/api/coworkings/${coworkingId}/booking/init`),
          requestJson<Booking[]>(`/api/coworkings/${coworkingId}/bookings`),
        ]);

        if (!isMounted) {
          return;
        }

        setCoworking(coworkingData);
        setBookingInit(bookingInitData);
        setBookings(bookingItems);
        setErrorMessage(null);
      } catch (error: unknown) {
        if (!isMounted) {
          return;
        }

        setErrorMessage(
          error instanceof ClientRequestError
            ? error.message
            : 'Не удалось загрузить данные коворкинга.',
        );
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadPageData();

    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  const membershipStatus = (coworking?.membershipStatus ?? bookingInit?.membershipStatus ?? 'pending') as MembershipStatus;
  const balance = coworking?.balanceMinorUnits ?? bookingInit?.balanceMinorUnits ?? 0;

  const statusContent = useMemo<StatusContent>(() => {
    if (membershipStatus === 'active') {
      return {
        title: coworking?.heroTitle ?? coworking?.name ?? 'Коворкинг',
        description: coworking?.heroText ?? coworking?.description ?? '',
      };
    }

    if (membershipStatus === 'blocked') {
      return {
        title: coworking?.heroTitle ?? coworking?.name ?? 'Коворкинг',
        description:
          'Создание бронирований и пополнение недоступны. Вы можете создавать сервисные заявки и заявки на списание средств, а также просматривать историю.',
      };
    }

    return {
      title: coworking?.heroTitle ?? coworking?.name ?? 'Коворкинг',
      description:
        'Вы можете просматривать пространство и общую доступность, но все действия временно заблокированы до подтверждения membership.',
    };
  }, [coworking, membershipStatus]);

  const quickActions = useMemo<QuickAction[]>(() => {
    return [
      {
        label: 'Новое бронирование',
        href: `/coworkings/${coworkingId}/bookings/new`,
        disabled: membershipStatus !== 'active',
        variant: 'primary',
      },
    ];
  }, [coworkingId, membershipStatus]);

  const nearbyBookings = useMemo(() => {
    return bookings.filter((item) => item.active).slice(0, 3);
  }, [bookings]);

  const showcaseSlides = useMemo(() => {
    if (!coworking) {
      return [];
    }

    return (coworking.imageUrls ?? []).map((imageUrl, index) => ({
      id: `${coworking.id}-${index + 1}`,
      title: `${coworking.name} — фото ${index + 1}`,
      image: imageUrl,
    }));
  }, [coworking]);

  useEffect(() => {
    if (showcaseSlides.length <= 1) {
      return;
    }

    let isDisposed = false;

    async function startCarousel() {
      const carouselElement = document.getElementById(`coworkingShowcase-${coworkingId}`);
      if (!carouselElement) {
        return;
      }

      const bootstrapModule = await import('bootstrap/js/dist/carousel');
      const Carousel = bootstrapModule.default;
      if (isDisposed) {
        return;
      }

      const instance = Carousel.getOrCreateInstance(carouselElement, {
        interval: 4000,
        ride: 'carousel',
        pause: false,
        touch: true,
        wrap: true,
      });

      instance.cycle();
    }

    void startCarousel();

    return () => {
      isDisposed = true;
      const carouselElement = document.getElementById(`coworkingShowcase-${coworkingId}`);
      if (!carouselElement) {
        return;
      }

      import('bootstrap/js/dist/carousel')
        .then((bootstrapModule) => {
          const Carousel = bootstrapModule.default;
          const instance = Carousel.getInstance(carouselElement);
          instance?.dispose();
        })
        .catch(() => {
          // Игнорируем ошибку очистки, чтобы не ломать страницу при размонтировании.
        });
    };
  }, [coworkingId, showcaseSlides.length]);

  if (isLoading) {
    return <div className="alert alert-secondary mb-0">Загрузка данных коворкинга...</div>;
  }

  if (errorMessage) {
    return <div className="alert alert-danger mb-0">{errorMessage}</div>;
  }

  if (!coworking || !bookingInit) {
    return <div className="alert alert-warning mb-0">Коворкинг не найден.</div>;
  }

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm overflow-hidden">
        <div className="card-body p-4 p-lg-5">
          <div className="row g-4 align-items-center">
            <div className="col-lg-7">
              <h1 className="display-6 fw-semibold mb-3">{statusContent.title}</h1>
              <p className="lead text-body-secondary mb-4">{statusContent.description}</p>
              <div className="d-flex flex-wrap gap-2">
                {quickActions.map((action) => (
                  <Link
                    key={action.label}
                    href={action.disabled ? '#' : action.href}
                    aria-disabled={action.disabled}
                    onClick={(event) => {
                      if (action.disabled) {
                        event.preventDefault();
                      }
                    }}
                    className={`btn btn-${action.variant} btn-lg${action.disabled ? ' disabled' : ''}`}
                  >
                    {action.label}
                  </Link>
                ))}
              </div>
            </div>

            <div className="col-lg-5">
              <div className="card border h-100">
                <div className="card-body">
                  <div className="text-body-secondary small mb-1">Адрес</div>
                  <div className="fw-semibold mb-3">{coworking.address}</div>
                  <div className="text-body-secondary small mb-1">Режим работы</div>
                  <div className="fw-semibold mb-0">{coworking.workingHoursLabel}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="row g-4">
        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-grid gap-4">
              <div>
                <div className="text-body-secondary small">Текущий баланс</div>
                <div className="display-6 fw-semibold">{formatMoney(balance)}</div>
              </div>

              <div>
                <h2 className="h5 mb-3">Ближайшие бронирования</h2>
                <div className="list-group list-group-flush">
                  {nearbyBookings.length === 0 ? (
                    <div className="text-body-secondary">Активных бронирований пока нет.</div>
                  ) : (
                    nearbyBookings.map((booking) => (
                      <div className="list-group-item px-0 py-3" key={booking.id}>
                        <div className="fw-semibold">{booking.placeName}</div>
                        <div className="text-body-secondary small">{formatDate(booking.date)}</div>
                        <div className="mt-2">{formatMoney(booking.cost)}</div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <Link href={`/coworkings/${coworkingId}/bookings`} className="btn btn-primary">
                Все бронирования
              </Link>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-7">
          {showcaseSlides.length === 0 ? (
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body p-4 d-flex align-items-center justify-content-center text-body-secondary">
                Фотографии коворкинга пока не добавлены.
              </div>
            </div>
          ) : (
            <div
              id={`coworkingShowcase-${coworkingId}`}
              className="carousel slide card border-0 shadow-sm overflow-hidden"
              data-bs-ride={showcaseSlides.length > 1 ? 'carousel' : undefined}
              data-bs-interval={showcaseSlides.length > 1 ? '4000' : undefined}
            >
              {showcaseSlides.length > 1 && (
                <div className="carousel-indicators mb-3">
                  {showcaseSlides.map((slide, index) => (
                    <button
                      key={slide.id}
                      type="button"
                      data-bs-target={`#coworkingShowcase-${coworkingId}`}
                      data-bs-slide-to={index}
                      className={index === 0 ? 'active' : ''}
                      aria-current={index === 0 ? 'true' : undefined}
                      aria-label={`Слайд ${index + 1}`}
                    />
                  ))}
                </div>
              )}

              <div className="carousel-inner">
                {showcaseSlides.map((slide, index) => (
                  <div key={slide.id} className={`carousel-item ${index === 0 ? 'active' : ''}`}>
                    <img
                      src={slide.image}
                      className="d-block w-100"
                      style={{ height: '420px', objectFit: 'cover' }}
                      alt={slide.title}
                    />
                  </div>
                ))}
              </div>

              {showcaseSlides.length > 1 && (
                <>
                  <button
                    className="carousel-control-prev"
                    type="button"
                    data-bs-target={`#coworkingShowcase-${coworkingId}`}
                    data-bs-slide="prev"
                  >
                    <span className="carousel-control-prev-icon" aria-hidden="true"/>
                    <span className="visually-hidden">Предыдущий слайд</span>
                  </button>
                  <button
                    className="carousel-control-next"
                    type="button"
                    data-bs-target={`#coworkingShowcase-${coworkingId}`}
                    data-bs-slide="next"
                  >
                    <span className="carousel-control-next-icon" aria-hidden="true"/>
                    <span className="visually-hidden">Следующий слайд</span>
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
