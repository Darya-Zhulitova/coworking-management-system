import { notFound } from 'next/navigation';
import { CoworkingFloorDetailPageClient } from '@/features/coworkings/ui/coworking-floor-detail-page-client';

export default async function CoworkingFloorDetailPage({ params }: {
  params: Promise<{ id: string; floorId: string }>
}) {
  const { id, floorId } = await params;
  const coworkingId = Number(id);
  const parsedFloorId = Number(floorId);
  if (!Number.isInteger(coworkingId) || !Number.isInteger(parsedFloorId)) notFound();
  return <CoworkingFloorDetailPageClient coworkingId={coworkingId} floorId={parsedFloorId}/>;
}
