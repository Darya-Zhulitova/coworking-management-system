import { redirect } from 'next/navigation';
import { getUserSession } from '@/lib/auth/session';
import { AuthPage } from '@/components/features/auth/auth-page';

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams;
  if (await getUserSession()) {
    redirect(next && next.startsWith('/') ? next : '/');
  }
  return <AuthPage mode="login"/>;
}
