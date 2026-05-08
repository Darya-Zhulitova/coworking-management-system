import { notFound } from 'next/navigation';
import {
  CoworkingServiceRequestDetailPageClient
} from '@/features/coworkings/ui/coworking-service-request-detail-page-client';

export default async function Page({ params }: { params: Promise<{ id: string; serviceRequestId: string }> }) {
  const { id, serviceRequestId } = await params;
  const coworkingId = Number(id);
  const parsedRequestId = Number(serviceRequestId);
  if (!Number.isInteger(coworkingId) || !Number.isInteger(parsedRequestId)) notFound();
  return <CoworkingServiceRequestDetailPageClient coworkingId={coworkingId} serviceRequestId={parsedRequestId}/>;
}
