import { notFound } from 'next/navigation';
import { getJoinCoworkingPreview } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';
import { JoinCoworkingPageClient } from '@/components/features/join/join-coworking-page-client';

export default async function JoinCoworkingPage({ params }: { params: Promise<{ joinToken: string }> }) {
  const { joinToken } = await params;
  if (!joinToken.trim()) notFound();

  const preview = await getJoinCoworkingPreview(joinToken).catch(() => null);
  if (!preview) notFound();

  const session = await getUserSession();
  return <JoinCoworkingPageClient preview={preview} joinToken={joinToken} isAuthenticated={session !== null}/>;
}
