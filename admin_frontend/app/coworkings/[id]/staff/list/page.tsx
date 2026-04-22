import { notFound } from 'next/navigation';
import { CoworkingStaffListPageClient } from '@/features/coworkings/ui/coworking-staff-list-page-client';

export default async function CoworkingStaffListPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingStaffListPageClient coworkingId={coworkingId}/>;
}
