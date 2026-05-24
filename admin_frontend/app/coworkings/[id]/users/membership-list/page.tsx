import { notFound } from 'next/navigation';
import { CoworkingMembershipListPageClient } from '@/features/coworkings/ui/coworking-membership-list-page-client';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const coworkingId = Number((await params).id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingMembershipListPageClient coworkingId={coworkingId}/>;
}
