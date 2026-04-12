import { Dashboard } from '@/components/features/home/dashboard';
import { requireUserSession } from '@/lib/auth/session';

export default async function HomePage() {
  await requireUserSession();
  return <Dashboard/>;
}
