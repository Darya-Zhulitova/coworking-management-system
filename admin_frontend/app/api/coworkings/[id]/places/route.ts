import { NextResponse } from 'next/server';
import { BackendRequestError, createPlace, getPlaces } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  const url = new URL(request.url);
  const placeTypeIdRaw = url.searchParams.get('placeTypeId');
  const placeTypeId = placeTypeIdRaw == null ? undefined : parseId(placeTypeIdRaw) ?? undefined;
  try {
    return NextResponse.json(await getPlaces(session.token, coworkingId, placeTypeId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load places.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: { name?: string; floorId?: number; placeTypeId?: number; locX?: number; locY?: number };
  try {
    payload = await request.json() as {
      name?: string;
      floorId?: number;
      placeTypeId?: number;
      locX?: number;
      locY?: number
    };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim() || !payload.floorId || !payload.placeTypeId) return NextResponse.json({ message: 'Name, floor and place type are required.' }, { status: 400 });
  try {
    return NextResponse.json(await createPlace(session.token, coworkingId, {
      name: payload.name.trim(),
      floorId: payload.floorId,
      placeTypeId: payload.placeTypeId,
      locX: payload.locX,
      locY: payload.locY,
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create place.' }, { status: 500 });
  }
}
