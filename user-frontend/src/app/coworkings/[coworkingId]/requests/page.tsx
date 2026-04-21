import { RequestsPage } from '@/components/features/requests/requests-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function RequestsRoute({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <RequestsPage coworkingId={Number(coworkingId)}/>;
}
