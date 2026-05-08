import { notFound } from 'next/navigation';
import { CoworkingTariffFormPageClient } from '@/features/coworkings/ui/coworking-tariff-form-page-client';

export default async function CoworkingTariffCreatePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingTariffFormPageClient coworkingId={coworkingId}/>;
}
