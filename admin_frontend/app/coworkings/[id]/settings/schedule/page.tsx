import { notFound } from 'next/navigation';
import { CoworkingSchedulePageClient } from '@/features/coworkings/ui/coworking-schedule-page-client';

export default async function CoworkingSchedulePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingSchedulePageClient coworkingId={coworkingId}/>;
}
