import { notFound } from 'next/navigation';
import { TenantDashboardPageClient } from '@/features/coworkings/ui/tenant-dashboard-page-client';

export default async function TenantDashboardPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <TenantDashboardPageClient coworkingId={coworkingId} />;
}
