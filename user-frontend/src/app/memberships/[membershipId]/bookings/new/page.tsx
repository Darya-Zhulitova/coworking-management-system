import { BookingNewPage } from '@/components/features/bookings/booking-new-page';
import { getBookingInit } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';
import type { BookingInitData } from '@/lib/types';

export default async function BookingNewRoute({ params }: { params: Promise<{ membershipId: string }> }) {
  const session = await requireUserSession();
  const { membershipId } = await params;
  const parsedMembershipId = Number(membershipId);
  let initData: BookingInitData | null = null;
  let initialError: string | null = null;

  try {
    initData = await getBookingInit(parsedMembershipId, session.token);
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить страницу бронирования.';
  }

  return <BookingNewPage membershipId={parsedMembershipId} initialData={initData} initialError={initialError}/>;
}
