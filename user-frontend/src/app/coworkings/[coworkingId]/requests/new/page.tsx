import { RequestNewPage } from '@/components/features/requests/request-new-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function RequestNewRoute({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <RequestNewPage coworkingId={Number(coworkingId)}/>;
}
