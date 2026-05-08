import { NextResponse } from 'next/server';
import { BackendRequestError, getPlaceBookings } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);

  if (coworkingId == null || parsedPlaceId == null) {
    return NextResponse.json({ message: 'Invalid coworking or place id.' }, { status: 400 });
  }

  try {
    return NextResponse.json(await getPlaceBookings(session.token, coworkingId, parsedPlaceId));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load place bookings.' }, { status: 500 });
  }
}
