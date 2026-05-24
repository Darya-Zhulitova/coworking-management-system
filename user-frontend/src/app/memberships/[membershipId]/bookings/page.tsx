import { BookingsPage } from '@/components/features/bookings/bookings-page';
import { getBookingInit, getBookings } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';
import type { Booking, BookingInitData } from '@/lib/types';

export default async function BookingsRoute({ params }: { params: Promise<{ membershipId: string }> }) {
  const session = await requireUserSession();
  const { membershipId } = await params;
  const parsedMembershipId = Number(membershipId);
  let bookingInit: BookingInitData | null = null;
  let bookings: Booking[] = [];
  let initialError: string | null = null;

  try {
    [bookingInit, bookings] = await Promise.all([
      getBookingInit(parsedMembershipId, session.token),
      getBookings(parsedMembershipId, session.token),
    ]);
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить бронирования.';
  }

  return <BookingsPage membershipId={parsedMembershipId} initialBookingInit={bookingInit} initialBookings={bookings}
                       initialError={initialError}/>;
}
