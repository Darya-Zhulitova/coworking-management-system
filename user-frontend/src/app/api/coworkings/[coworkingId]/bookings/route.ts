import { getBookings } from '@/lib/api/backend';
import { handleApiError, okJson, parseCoworkingId, requireApiSession } from '@/lib/api/route-helpers';

export async function GET(_request: Request, { params }: { params: Promise<{ coworkingId: string }> }) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const parsedCoworkingId = parseCoworkingId((await params).coworkingId);
  if (!parsedCoworkingId.ok) return parsedCoworkingId.response;

  try {
    return okJson(await getBookings(parsedCoworkingId.value, session.value.token));
  } catch (error) {
    return handleApiError(error, 'Unable to load bookings.');
  }
}
