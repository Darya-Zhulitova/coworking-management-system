import { ProfilePage } from '@/components/features/profile/profile-page';
import { getCurrentUser, getCurrentUserMemberships } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';
import type { MembershipSummary, UserProfile } from '@/lib/types';

export default async function ProfileRoute() {
  const session = await requireUserSession();
  let profile: UserProfile | null = null;
  let memberships: MembershipSummary[] = [];
  let initialError: string | null = null;

  try {
    [profile, memberships] = await Promise.all([
      getCurrentUser(session.token),
      getCurrentUserMemberships(session.token),
    ]);
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить профиль.';
  }

  return <ProfilePage selectedMembershipId={null} initialProfile={profile} initialMemberships={memberships}
                      initialError={initialError}/>;
}
