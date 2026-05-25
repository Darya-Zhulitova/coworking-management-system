import { notFound } from 'next/navigation';
import {
  CoworkingMembershipProfilePageClient
} from '@/features/coworkings/ui/coworking-membership-profile-page-client';

export default async function Page({ params }: { params: Promise<{ id: string; membershipId: string }> }) {
  const { id, membershipId } = await params;
  const coworkingId = Number(id);
  const parsedMembershipId = Number(membershipId);
  if (!Number.isInteger(coworkingId) || !Number.isInteger(parsedMembershipId)) notFound();
  return <CoworkingMembershipProfilePageClient coworkingId={coworkingId} membershipId={parsedMembershipId}/>;
}
