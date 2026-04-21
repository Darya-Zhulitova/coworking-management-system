import { NextResponse } from 'next/server';
import { BackendRequestError, cancelBooking } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

function parsePositiveId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export async function POST(_request: Request, { params }: {
  params: Promise<{ coworkingId: string; bookingId: string }>
}) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const { coworkingId: rawCoworkingId, bookingId: rawBookingId } = await params;
  const coworkingId = parsePositiveId(rawCoworkingId);
  const bookingId = parsePositiveId(rawBookingId);
  if (coworkingId == null || bookingId == null) return NextResponse.json({ message: 'Invalid request parameters.' }, { status: 400 });

  try {
    return NextResponse.json(await cancelBooking(coworkingId, bookingId, session.token));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to cancel booking.' }, { status: 500 });
  }
}
