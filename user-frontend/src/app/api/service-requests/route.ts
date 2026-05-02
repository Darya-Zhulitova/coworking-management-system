import { createServiceRequest } from '@/lib/api/backend';
import {
  badRequest,
  createdJson,
  handleApiError,
  parsePositiveInteger,
  readJsonBody,
  requireApiSession,
} from '@/lib/api/route-helpers';

type CreateServiceRequestPayload = {
  coworkingId?: number;
  typeId?: number;
  name?: string;
};

export async function POST(request: Request) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const body = await readJsonBody<CreateServiceRequestPayload>(request);
  if (!body.ok) return body.response;

  const payload = body.value;
  const coworkingId = parsePositiveInteger(payload.coworkingId);
  if (coworkingId == null) return badRequest('Invalid coworking id.');

  const typeId = parsePositiveInteger(payload.typeId);
  if (typeId == null) return badRequest('Invalid request type.');

  if (!payload.name?.trim()) return badRequest('Name is required.');

  try {
    const created = await createServiceRequest({
      coworkingId,
      typeId,
      name: payload.name.trim(),
    }, session.value.token);
    return createdJson(created);
  } catch (error) {
    return handleApiError(error, 'Unable to create service request.');
  }
}
