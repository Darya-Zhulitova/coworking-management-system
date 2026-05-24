import { RequestsPage } from '@/components/features/requests/requests-page';
import { type BackendServiceRequest, getCoworkingContext, getServiceRequests } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';

export default async function RequestsRoute({ params }: { params: Promise<{ membershipId: string }> }) {
  const session = await requireUserSession();
  const { membershipId } = await params;
  const parsedMembershipId = Number(membershipId);
  let requests: BackendServiceRequest[] = [];
  let membershipStatus: string | null = null;
  let initialError: string | null = null;

  try {
    const [requestItems, context] = await Promise.all([
      getServiceRequests(parsedMembershipId, session.token),
      getCoworkingContext(parsedMembershipId, session.token),
    ]);
    requests = requestItems;
    membershipStatus = context.membership.status;
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить сервисные заявки.';
  }

  return <RequestsPage membershipId={parsedMembershipId} initialRequests={requests}
                       initialMembershipStatus={membershipStatus} initialError={initialError}/>;
}
