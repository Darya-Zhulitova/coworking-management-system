import { notFound } from 'next/navigation';
import { CoworkingMembershipQueuePageClient } from '@/features/coworkings/ui/coworking-membership-queue-page-client';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const coworkingId = Number((await params).id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingMembershipQueuePageClient coworkingId={coworkingId}/>;
}
