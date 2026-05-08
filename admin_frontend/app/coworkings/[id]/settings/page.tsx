import { notFound } from 'next/navigation';
import { CoworkingSettingsPageClient } from '@/features/coworkings/ui/coworking-settings-page-client';

export default async function CoworkingSettingsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingSettingsPageClient coworkingId={coworkingId}/>;
}
