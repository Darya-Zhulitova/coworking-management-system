import { cancelBooking } from '@/lib/api/backend';
import { badRequest, handleApiError, okJson, parsePositiveInteger, requireApiSession } from '@/lib/api/route-helpers';

export async function POST(_request: Request, { params }: {
  params: Promise<{ coworkingId: string; bookingId: string }>
}) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const { coworkingId: rawCoworkingId, bookingId: rawBookingId } = await params;
  const coworkingId = parsePositiveInteger(rawCoworkingId);
  const bookingId = parsePositiveInteger(rawBookingId);
  if (coworkingId == null || bookingId == null) return badRequest('Invalid request parameters.');

  try {
    return okJson(await cancelBooking(coworkingId, bookingId, session.value.token));
  } catch (error) {
    return handleApiError(error, 'Unable to cancel booking.');
  }
}
