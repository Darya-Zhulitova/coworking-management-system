import { BookingNewPage } from '@/components/features/bookings/booking-new-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function BookingNewRoute({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <BookingNewPage coworkingId={Number(coworkingId)}/>;
}
