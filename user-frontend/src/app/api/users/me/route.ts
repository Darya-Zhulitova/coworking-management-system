import { getCurrentUser } from '@/lib/api/backend';
import { handleApiError, okJson, requireApiSession } from '@/lib/api/route-helpers';

export async function GET() {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  try {
    return okJson(await getCurrentUser(session.value.token));
  } catch (error) {
    return handleApiError(error, 'Unable to load user.');
  }
}
