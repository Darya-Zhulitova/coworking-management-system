import {ProfilePage} from '@/components/features/profile/profile-page';

export default async function CoworkingProfileRoute({params}: {params: Promise<{coworkingId: string}>}) {
  const {coworkingId} = await params;
  return <ProfilePage selectedCoworkingId={Number(coworkingId)}/>;
}
