import { redirect } from 'next/navigation';
import { requireAdminSession } from '@/lib/auth/session';

export default async function DashboardPage() {
  const session = await requireAdminSession();
  const firstCoworking = session.coworkings[0];
  if (firstCoworking) {
    redirect(`/coworkings/${firstCoworking.id}/dashboard`);
  }
  redirect('/coworkings');
}
