import { RequestNewPage } from '@/components/features/requests/request-new-page';
import { type BackendServiceRequestTypeOption, getCoworkingContext, getServiceRequestTypes } from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';

export default async function RequestNewRoute({ params }: { params: Promise<{ membershipId: string }> }) {
  const session = await requireUserSession();
  const { membershipId } = await params;
  const parsedMembershipId = Number(membershipId);
  let requestTypes: BackendServiceRequestTypeOption[] = [];
  let membershipStatus: string | null = null;
  let initialError: string | null = null;

  try {
    const [context, types] = await Promise.all([
      getCoworkingContext(parsedMembershipId, session.token),
      getServiceRequestTypes(parsedMembershipId, session.token),
    ]);
    membershipStatus = context.membership.status;
    requestTypes = types;
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить данные для сервисной заявки.';
  }

  return <RequestNewPage membershipId={parsedMembershipId} initialMembershipStatus={membershipStatus}
                         initialRequestTypes={requestTypes} initialError={initialError}/>;
}
