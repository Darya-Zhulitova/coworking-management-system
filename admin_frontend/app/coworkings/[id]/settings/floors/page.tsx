import { notFound } from 'next/navigation';
import { CoworkingFloorsPageClient } from '@/features/coworkings/ui/coworking-floors-page-client';

export default async function CoworkingFloorsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingFloorsPageClient coworkingId={coworkingId}/>;
}
