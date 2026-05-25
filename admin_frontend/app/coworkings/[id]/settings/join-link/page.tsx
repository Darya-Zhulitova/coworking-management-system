import { notFound } from 'next/navigation';
import { CoworkingJoinLinkPageClient } from '@/features/coworkings/ui/coworking-join-link-page-client';

export default async function CoworkingJoinLinkPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingJoinLinkPageClient coworkingId={coworkingId}/>;
}
