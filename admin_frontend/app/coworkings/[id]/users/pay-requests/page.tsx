import { notFound } from 'next/navigation';
import { CoworkingPayRequestsPageClient } from '@/features/coworkings/ui/coworking-pay-requests-page-client';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const coworkingId = Number((await params).id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingPayRequestsPageClient coworkingId={coworkingId}/>;
}
