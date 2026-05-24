import { BalancePage } from '@/components/features/balance/balance-page';
import { getBalanceDetails } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';

export default async function BalanceRoute({ params }: { params: Promise<{ membershipId: string }> }) {
  const session = await requireUserSession();
  const { membershipId } = await params;
  const parsedMembershipId = Number(membershipId);
  let initialData: React.ComponentProps<typeof BalancePage>['initialData'] = null;
  let initialError: string | null = null;

  try {
    const balance = await getBalanceDetails(parsedMembershipId, session.token);
    initialData = {
      ...balance,
      membershipStatus: String(balance.membershipStatus).toLowerCase() as 'active' | 'pending' | 'blocked',
    };
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить данные баланса.';
  }

  return <BalancePage membershipId={parsedMembershipId} initialData={initialData} initialError={initialError}/>;
}
