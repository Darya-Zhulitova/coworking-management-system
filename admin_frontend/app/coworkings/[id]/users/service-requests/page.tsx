import { notFound } from 'next/navigation';
import { CoworkingServiceRequestsPageClient } from '@/features/coworkings/ui/coworking-service-requests-page-client';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const coworkingId = Number((await params).id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingServiceRequestsPageClient coworkingId={coworkingId}/>;
}
