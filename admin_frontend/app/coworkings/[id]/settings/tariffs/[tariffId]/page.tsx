import { notFound } from 'next/navigation';
import { CoworkingTariffFormPageClient } from '@/features/coworkings/ui/coworking-tariff-form-page-client';

export default async function CoworkingTariffEditPage({ params }: {
  params: Promise<{ id: string; tariffId: string }>
}) {
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = Number(id);
  const tariffId = Number(tariffIdRaw);
  if (!Number.isInteger(coworkingId) || !Number.isInteger(tariffId)) notFound();
  return <CoworkingTariffFormPageClient coworkingId={coworkingId} tariffId={tariffId}/>;
}
