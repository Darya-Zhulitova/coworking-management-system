import { notFound } from 'next/navigation';
import { CoworkingUsersPageClient } from '@/features/coworkings/ui/coworking-users-page-client';

export default async function CoworkingUsersPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingUsersPageClient coworkingId={coworkingId}/>;
}
