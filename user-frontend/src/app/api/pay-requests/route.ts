import { createPayRequest } from '@/lib/api/backend';
import {
  badRequest,
  createdJson,
  handleApiError,
  parsePositiveInteger,
  readJsonBody,
  requireApiSession,
} from '@/lib/api/route-helpers';

type CreatePayRequestPayload = {
  coworkingId?: number;
  amount?: number;
  userComment?: string;
};

export async function POST(request: Request) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const body = await readJsonBody<CreatePayRequestPayload>(request);
  if (!body.ok) return body.response;

  const payload = body.value;
  const coworkingId = parsePositiveInteger(payload.coworkingId);
  if (coworkingId == null) return badRequest('Invalid coworking id.');

  if (!Number.isInteger(payload.amount) || Number(payload.amount) === 0) {
    return badRequest('Amount must be a non-zero integer.');
  }

  if (!payload.userComment?.trim()) {
    return badRequest('Comment is required.');
  }

  try {
    const created = await createPayRequest({
      coworkingId,
      amount: Number(payload.amount),
      userComment: payload.userComment.trim(),
    }, session.value.token);
    return createdJson(created);
  } catch (error) {
    return handleApiError(error, 'Unable to create pay request.');
  }
}
