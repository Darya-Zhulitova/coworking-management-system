import { getServiceRequestMessages } from '@/lib/api/backend';
import { badRequest, handleApiError, okJson, parsePositiveInteger, requireApiSession } from '@/lib/api/route-helpers';

export async function GET(_request: Request, { params }: {
  params: Promise<{ coworkingId: string; requestId: string }>
}) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const { coworkingId: rawCoworkingId, requestId: rawRequestId } = await params;
  const coworkingId = parsePositiveInteger(rawCoworkingId);
  const requestId = parsePositiveInteger(rawRequestId);
  if (coworkingId == null || requestId == null) return badRequest('Invalid request parameters.');

  try {
    return okJson(await getServiceRequestMessages(coworkingId, requestId, session.value.token));
  } catch (error) {
    return handleApiError(error, 'Unable to load request messages.');
  }
}
