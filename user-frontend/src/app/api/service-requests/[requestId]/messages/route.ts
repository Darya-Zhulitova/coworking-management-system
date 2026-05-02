import { createServiceRequestMessage } from '@/lib/api/backend';
import {
  badRequest,
  createdJson,
  handleApiError,
  parsePositiveInteger,
  readJsonBody,
  requireApiSession,
} from '@/lib/api/route-helpers';

type CreateMessagePayload = {
  coworkingId?: number;
  text?: string;
};

export async function POST(request: Request, { params }: { params: Promise<{ requestId: string }> }) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const requestId = parsePositiveInteger((await params).requestId);
  if (requestId == null) return badRequest('Invalid request id.');

  const body = await readJsonBody<CreateMessagePayload>(request);
  if (!body.ok) return body.response;

  const payload = body.value;
  const coworkingId = parsePositiveInteger(payload.coworkingId);
  if (coworkingId == null) return badRequest('Invalid coworking id.');

  if (!payload.text?.trim()) return badRequest('Message text is required.');

  try {
    const created = await createServiceRequestMessage({
      coworkingId,
      requestId,
      text: payload.text.trim(),
    }, session.value.token);
    return createdJson(created);
  } catch (error) {
    return handleApiError(error, 'Unable to send message.');
  }
}
