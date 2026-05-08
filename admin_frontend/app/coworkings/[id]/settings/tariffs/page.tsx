import { notFound } from 'next/navigation';
import { CoworkingTariffsPageClient } from '@/features/coworkings/ui/coworking-tariffs-page-client';

export default async function CoworkingTariffsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingTariffsPageClient coworkingId={coworkingId}/>;
}
