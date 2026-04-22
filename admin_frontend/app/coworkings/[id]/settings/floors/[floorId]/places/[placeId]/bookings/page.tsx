import { notFound } from 'next/navigation';
import { PlaceBookingsPageClient } from '@/features/coworkings/ui/place-bookings-page-client';

export default async function PlaceBookingsPage({ params }: {
  params: Promise<{ id: string; floorId: string; placeId: string }>
}) {
  const { id, floorId, placeId } = await params;
  const coworkingId = Number(id);
  const parsedFloorId = Number(floorId);
  const parsedPlaceId = Number(placeId);

  if (!Number.isInteger(coworkingId) || !Number.isInteger(parsedFloorId) || !Number.isInteger(parsedPlaceId)) {
    notFound();
  }

  return <PlaceBookingsPageClient coworkingId={coworkingId} floorId={parsedFloorId} placeId={parsedPlaceId}/>;
}
