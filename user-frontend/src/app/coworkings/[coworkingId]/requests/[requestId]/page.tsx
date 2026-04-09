import {RequestDetailPage} from '@/components/features/requests/request-detail-page';

export default async function RequestDetailRoute({params}: { params: Promise<{ coworkingId: string; requestId: string }> }) {
  const {coworkingId, requestId} = await params;
  return <RequestDetailPage coworkingId={Number(coworkingId)} requestId={Number(requestId)}/>;
}
