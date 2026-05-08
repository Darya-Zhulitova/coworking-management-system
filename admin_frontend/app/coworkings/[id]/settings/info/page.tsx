import { notFound } from 'next/navigation';
import { CoworkingDetailsPageClient } from '@/features/coworkings/ui/coworking-details-page-client';

export default async function CoworkingSettingsInfoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingDetailsPageClient coworkingId={coworkingId}/>;
}
