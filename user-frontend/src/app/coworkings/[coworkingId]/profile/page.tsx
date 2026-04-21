import { ProfilePage } from '@/components/features/profile/profile-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function CoworkingProfileRoute({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <ProfilePage selectedCoworkingId={Number(coworkingId)}/>;
}
