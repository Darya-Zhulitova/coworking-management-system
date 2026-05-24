import { CoworkingPageContent } from '@/components/features/coworkings/coworking-page-content';
import { getBookingInit, getBookings, getCoworking } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';
import type { Booking, BookingInitData, UserCoworkingDetails } from '@/lib/types';

export default async function CoworkingPage({ params }: { params: Promise<{ membershipId: string }> }) {
  const session = await requireUserSession();
  const { membershipId } = await params;
  const parsedMembershipId = Number(membershipId);
  let coworking: UserCoworkingDetails | null = null;
  let bookingInit: BookingInitData | null = null;
  let bookings: Booking[] = [];
  let initialError: string | null = null;

  try {
    [coworking, bookingInit, bookings] = await Promise.all([
      getCoworking(parsedMembershipId, session.token),
      getBookingInit(parsedMembershipId, session.token),
      getBookings(parsedMembershipId, session.token),
    ]);
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить данные коворкинга.';
  }

  return (
    <CoworkingPageContent
      membershipId={parsedMembershipId}
      initialCoworking={coworking}
      initialBookingInit={bookingInit}
      initialBookings={bookings}
      initialError={initialError}
    />
  );
}
