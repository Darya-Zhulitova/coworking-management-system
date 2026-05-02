import { RequestDetailPage } from '@/components/features/requests/request-detail-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function RequestDetailRoute({ params }: {
  params: Promise<{ coworkingId: string; requestId: string }>
}) {
  await requireUserSession();
  const { coworkingId, requestId } = await params;
  return <RequestDetailPage coworkingId={Number(coworkingId)} requestId={Number(requestId)}/>;
}
