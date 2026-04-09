import {BalancePage} from '@/components/features/balance/balance-page';

export default async function BalanceRoute({params}: { params: Promise<{ coworkingId: string }> }) {
  const {coworkingId} = await params;
  return <BalancePage coworkingId={Number(coworkingId)}/>;
}
