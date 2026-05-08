import { notFound } from 'next/navigation';
import { CoworkingPlaceTypesPageClient } from '@/features/coworkings/ui/coworking-place-types-page-client';

export default async function CoworkingPlaceTypesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingPlaceTypesPageClient coworkingId={coworkingId}/>;
}
