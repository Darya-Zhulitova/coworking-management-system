import { NextResponse } from 'next/server';
import { archivePlace, BackendRequestError, updatePlace } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, placeId: placeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeId = parseId(placeIdRaw);
  if (coworkingId == null || placeId == null) return NextResponse.json({ message: 'Invalid place path.' }, { status: 400 });
  let payload: { name?: string; locX?: number | null; locY?: number | null; amenities?: string[]; active?: boolean };
  try {
    payload = await request.json() as {
      name?: string;
      locX?: number | null;
      locY?: number | null;
      amenities?: string[];
      active?: boolean
    };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await updatePlace(session.token, coworkingId, placeId, {
      name: payload.name.trim(),
      locX: payload.locX,
      locY: payload.locY,
      amenities: payload.amenities,
      active: payload.active,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update place.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, placeId: placeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeId = parseId(placeIdRaw);
  if (coworkingId == null || placeId == null) return NextResponse.json({ message: 'Invalid place path.' }, { status: 400 });
  try {
    await archivePlace(session.token, coworkingId, placeId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive place.' }, { status: 500 });
  }
}
