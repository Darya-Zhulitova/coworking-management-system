import { Dashboard } from '@/components/features/home/dashboard';
import { getCurrentUserMemberships } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';
import type { MembershipSummary } from '@/lib/types';

export default async function HomePage() {
  const session = await requireUserSession();
  let memberships: MembershipSummary[] = [];
  let initialError: string | null = null;

  try {
    memberships = await getCurrentUserMemberships(session.token);
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить список коворкингов.';
  }

  return <Dashboard initialMemberships={memberships} initialError={initialError}/>;
}
