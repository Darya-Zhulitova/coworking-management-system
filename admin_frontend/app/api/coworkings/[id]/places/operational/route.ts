import { NextResponse } from 'next/server';
import { BackendRequestError, getPlaceOperationalItems } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string | null): number | null {
  if (!value) return null;
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  const floorId = parseId(new URL(request.url).searchParams.get('floorId'));
  try {
    return NextResponse.json(await getPlaceOperationalItems(session.token, coworkingId, floorId ?? undefined));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load place operational data.' }, { status: 500 });
  }
}
