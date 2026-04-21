import { NextResponse } from 'next/server';
import { BackendRequestError, getCoworking } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

function parseCoworkingId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ coworkingId: string }> }) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const coworkingId = parseCoworkingId((await params).coworkingId);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });

  try {
    return NextResponse.json(await getCoworking(coworkingId, session.token));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load coworking.' }, { status: 500 });
  }
}
