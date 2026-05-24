'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { formatDate, formatMoney } from '@/lib/format';
import type { Booking, BookingInitData, MembershipStatus, UserCoworkingDetails } from '@/lib/types';
import { ImagePreviewModal } from '@/components/ui/image-preview-modal';

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

export function CoworkingPageContent({
                                       membershipId,
                                       initialCoworking,
                                       initialBookingInit,
                                       initialBookings,
                                       initialError = null
                                     }: {
  membershipId: number;
  initialCoworking: UserCoworkingDetails | null;
  initialBookingInit: BookingInitData | null;
  initialBookings: Booking[];
  initialError?: string | null
}) {
  const [coworking] = useState<UserCoworkingDetails | null>(initialCoworking);
  const [bookingInit] = useState<BookingInitData | null>(initialBookingInit);
  const [bookings] = useState<Booking[]>(initialBookings);
  const [errorMessage] = useState<string | null>(initialError);
  const [previewImage, setPreviewImage] = useState<{ url: string; title: string } | null>(null);

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
          'Создание бронирований и пополнение недоступны. Вы можете создавать сервисные заявки и платежные заявки на списание средств, а также просматривать историю.',
      };
    }

    return {
      title: coworking?.heroTitle ?? coworking?.name ?? 'Коворкинг',
      description:
        'Вы можете просматривать пространство и общую доступность, но действия будут доступны после подтверждения доступа к коворкингу.',
    };
  }, [coworking, membershipStatus]);

  const quickActions = useMemo<QuickAction[]>(() => {
    return [
      {
        label: 'Новое бронирование',
        href: `/memberships/${membershipId}/bookings/new`,
        disabled: membershipStatus !== 'active',
        variant: 'primary',
      },
    ];
  }, [membershipId, membershipStatus]);

  const nearbyBookings = useMemo(() => {
    return bookings.filter((item) => item.active).slice(0, 3);
  }, [bookings]);

  const showcaseSlides = useMemo(() => {
    if (!coworking) {
      return [];
    }

    return (coworking.imageUrls ?? []).map((imageUrl, index) => ({
      id: `${membershipId}-${index + 1}`,
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
      const carouselElement = document.getElementById(`coworkingShowcase-${membershipId}`);
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
      const carouselElement = document.getElementById(`coworkingShowcase-${membershipId}`);
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
        });
    };
  }, [membershipId, showcaseSlides.length]);


  if (errorMessage) {
    return <div
      className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 mb-0">{errorMessage}</div>;
  }

  if (!coworking || !bookingInit) {
    return <div
      className="border rounded-4 border-warning-subtle bg-warning-subtle text-warning-emphasis p-3 mb-0">Коворкинг не
      найден.</div>;
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
                        <div className="d-flex gap-3 align-items-start">
                          {booking.placePreviewImageUrl ? (
                            <button type="button" className="btn p-0 border-0 bg-transparent flex-shrink-0"
                                    onClick={() => setPreviewImage({
                                      url: booking.placeFullImageUrl ?? booking.placePreviewImageUrl!,
                                      title: booking.placeName
                                    })}>
                              {/* eslint-disable-next-line @next/next/no-img-element */}
                              <img src={booking.placePreviewImageUrl} alt={booking.placeName}
                                   className="rounded object-fit-cover" width={96} height={72}/>
                            </button>
                          ) : null}
                          <div className="min-w-0">
                            <div className="fw-semibold">{booking.placeName}</div>
                            <div className="text-body-secondary small">{formatDate(booking.date)}</div>
                            <div className="mt-2">{formatMoney(booking.cost)}</div>
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <Link href={`/memberships/${membershipId}/bookings`}
                    className="btn btn-primary btn-lg align-self-start px-5">
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
              id={`coworkingShowcase-${membershipId}`}
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
                      data-bs-target={`#coworkingShowcase-${membershipId}`}
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
                    data-bs-target={`#coworkingShowcase-${membershipId}`}
                    data-bs-slide="prev"
                  >
                    <span className="carousel-control-prev-icon" aria-hidden="true"/>
                    <span className="visually-hidden">Предыдущий слайд</span>
                  </button>
                  <button
                    className="carousel-control-next"
                    type="button"
                    data-bs-target={`#coworkingShowcase-${membershipId}`}
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
      <ImagePreviewModal image={previewImage} onClose={() => setPreviewImage(null)}/>

    </div>
  );
}
