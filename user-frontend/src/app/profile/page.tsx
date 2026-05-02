import { ProfilePage } from '@/components/features/profile/profile-page';
import { requireUserSession } from '@/lib/auth/session';

export default async function ProfileRoute() {
  await requireUserSession();
  return <ProfilePage selectedCoworkingId={null}/>;
}
