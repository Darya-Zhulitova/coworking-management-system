import { notFound } from 'next/navigation';
import { TenantStaffPageClient } from '@/features/coworkings/ui/tenant-staff-page-client';

export default async function TenantStaffPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <TenantStaffPageClient coworkingId={coworkingId} />;
}
