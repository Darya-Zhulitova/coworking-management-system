import { NextResponse } from 'next/server';
import { BackendRequestError, getBookingInit } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

function parseCoworkingId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export async function GET(request: Request, { params }: { params: Promise<{ coworkingId: string }> }) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const coworkingId = parseCoworkingId((await params).coworkingId);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });

  try {
    const { searchParams } = new URL(request.url);
    const date = searchParams.get('date') ?? undefined;
    return NextResponse.json(await getBookingInit(coworkingId, session.token, date));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load booking init data.' }, { status: 500 });
  }
}
