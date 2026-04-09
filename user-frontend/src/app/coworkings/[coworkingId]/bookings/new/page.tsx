import {BookingNewPage} from '@/components/features/bookings/booking-new-page';

export default async function BookingNewRoute({params}: { params: Promise<{ coworkingId: string }> }) {
  const {coworkingId} = await params;
  return <BookingNewPage coworkingId={Number(coworkingId)}/>;
}
