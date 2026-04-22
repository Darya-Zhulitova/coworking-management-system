import { notFound } from 'next/navigation';
import {
  CoworkingServiceRequestTypesPageClient
} from '@/features/coworkings/ui/coworking-service-request-types-page-client';

export default async function CoworkingServiceRequestTypesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const coworkingId = Number(id);
  if (!Number.isInteger(coworkingId)) notFound();
  return <CoworkingServiceRequestTypesPageClient coworkingId={coworkingId}/>;
}
