import { notFound } from 'next/navigation';
import { CoworkingUsersListPageClient } from '@/features/coworkings/ui/coworking-users-list-page-client';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const coworkingId = Number((await params).id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingUsersListPageClient coworkingId={coworkingId}/>;
}
