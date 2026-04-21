import { BookingsPage } from '@/components/features/bookings/bookings-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function BookingsRoute({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <BookingsPage coworkingId={Number(coworkingId)}/>;
}
