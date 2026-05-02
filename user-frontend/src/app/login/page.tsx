import { redirect } from 'next/navigation';
import { getUserSession } from '@/lib/auth/session';
import { AuthPage } from '@/components/features/auth/auth-page';

export default async function LoginPage() {
  if (await getUserSession()) {
    redirect('/');
  }
  return <AuthPage mode="login"/>;
}
