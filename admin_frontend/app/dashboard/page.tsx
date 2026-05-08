import { redirect } from 'next/navigation';
import { requireAdminSession } from '@/lib/auth/session';

export default async function DashboardPage() {
  await requireAdminSession();
  redirect('/coworkings');
}
