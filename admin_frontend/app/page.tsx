import { redirect } from 'next/navigation';
import { getAdminSession } from '@/lib/auth/session';
import { env } from '@/lib/config/env';
import { HomePageClient } from '@/features/home/ui/home-page-client';

export default async function HomePage() {
  const session = await getAdminSession();

  if (session) {
    redirect('/coworkings');
  }

  return <HomePageClient apiBaseUrl={env.apiBaseUrl}/>;
}