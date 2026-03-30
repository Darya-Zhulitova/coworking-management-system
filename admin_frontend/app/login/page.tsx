import { redirect } from 'next/navigation';
import { LoginForm } from '@/features/auth/ui/login-form';
import { getAdminSession } from '@/lib/auth/session';

export default async function LoginPage() {
  const session = await getAdminSession();
  if (session) redirect('/dashboard');

  return (
    <main className="page auth-page">
      <LoginForm />
    </main>
  );
}
