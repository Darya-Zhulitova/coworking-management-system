import { getBookingInit } from '@/lib/api/backend';
import { handleApiError, okJson, parseCoworkingId, requireApiSession } from '@/lib/api/route-helpers';

export async function GET(request: Request, { params }: { params: Promise<{ coworkingId: string }> }) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const parsedCoworkingId = parseCoworkingId((await params).coworkingId);
  if (!parsedCoworkingId.ok) return parsedCoworkingId.response;

  try {
    const { searchParams } = new URL(request.url);
    const date = searchParams.get('date') ?? undefined;
    return okJson(await getBookingInit(parsedCoworkingId.value, session.value.token, date));
  } catch (error) {
    return handleApiError(error, 'Unable to load booking init data.');
  }
}
