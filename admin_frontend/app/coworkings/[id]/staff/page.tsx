import { notFound } from 'next/navigation';
import { CoworkingStaffPageClient } from '@/features/coworkings/ui/coworking-staff-page-client';

export default async function CoworkingStaffPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingStaffPageClient coworkingId={coworkingId}/>;
}
