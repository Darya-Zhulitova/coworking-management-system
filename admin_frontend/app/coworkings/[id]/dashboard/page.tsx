import { notFound } from 'next/navigation';
import { CoworkingDashboardPageClient } from '@/features/coworkings/ui/coworking-dashboard-page-client';

export default async function CoworkingDashboardPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingDashboardPageClient coworkingId={coworkingId} />;
}
