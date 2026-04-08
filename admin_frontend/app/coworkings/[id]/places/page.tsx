import { notFound } from 'next/navigation';
import { TenantPlacesPageClient } from '@/features/coworkings/ui/tenant-places-page-client';

export default async function TenantPlacesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <TenantPlacesPageClient coworkingId={coworkingId} />;
}
