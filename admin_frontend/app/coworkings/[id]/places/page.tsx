import { notFound } from 'next/navigation';
import { CoworkingPlacesPageClient } from '@/features/coworkings/ui/coworking-places-page-client';

export default async function CoworkingPlacesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingPlacesPageClient coworkingId={coworkingId} />;
}
