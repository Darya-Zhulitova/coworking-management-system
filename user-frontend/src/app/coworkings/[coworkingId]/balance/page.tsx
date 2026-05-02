import { BalancePage } from '@/components/features/balance/balance-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function BalanceRoute({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <BalancePage coworkingId={Number(coworkingId)}/>;
}
