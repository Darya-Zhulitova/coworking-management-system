import { RequestDetailPage } from '@/components/features/requests/request-detail-page';
import {
  type BackendServiceRequest,
  type BackendServiceRequestMessage,
  getCoworkingContext,
  getServiceRequest,
  getServiceRequestMessages,
} from '@/lib/api/backend';
import { requireUserSession } from '@/lib/auth/session';

export default async function RequestDetailRoute({ params }: {
  params: Promise<{ membershipId: string; requestId: string }>
}) {
  const session = await requireUserSession();
  const { membershipId, requestId } = await params;
  const parsedMembershipId = Number(membershipId);
  const parsedRequestId = Number(requestId);
  let request: BackendServiceRequest | null = null;
  let messages: BackendServiceRequestMessage[] = [];
  let membershipStatus: 'active' | 'pending' | 'blocked' | null = null;
  let initialError: string | null = null;

  try {
    const [requestItem, requestMessages, context] = await Promise.all([
      getServiceRequest(parsedMembershipId, parsedRequestId, session.token),
      getServiceRequestMessages(parsedMembershipId, parsedRequestId, session.token),
      getCoworkingContext(parsedMembershipId, session.token),
    ]);
    request = requestItem;
    messages = requestMessages;
    membershipStatus = context.membership.status;
  } catch (error) {
    initialError = error instanceof Error ? error.message : 'Не удалось загрузить сервисную заявку.';
  }

  return (
    <RequestDetailPage
      membershipId={parsedMembershipId}
      requestId={parsedRequestId}
      initialRequest={request}
      initialMessages={messages}
      initialMembershipStatus={membershipStatus}
      initialError={initialError}
    />
  );
}
