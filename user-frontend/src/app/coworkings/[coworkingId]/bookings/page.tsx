import {BookingsPage} from '@/components/features/bookings/bookings-page';

export default async function BookingsRoute({params}: { params: Promise<{ coworkingId: string }> }) {
  const {coworkingId} = await params;
  return <BookingsPage coworkingId={Number(coworkingId)}/>;
}
