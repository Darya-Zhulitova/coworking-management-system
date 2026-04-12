import { redirect } from 'next/navigation';
import { getUserSession } from '@/lib/auth/session';
import { AuthPage } from '@/components/features/auth/auth-page';

export default async function RegisterPage() {
  if (await getUserSession()) {
    redirect('/');
  }
  return <AuthPage mode="register"/>;
}
