import { notFound } from 'next/navigation';
import { CoworkingRolesPageClient } from '@/features/coworkings/ui/coworking-roles-page-client';

export default async function CoworkingRolesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingRolesPageClient coworkingId={coworkingId}/>;
}
