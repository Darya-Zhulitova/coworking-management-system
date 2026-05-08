import { NextResponse } from 'next/server';
import { archivePlaceType, BackendRequestError, updatePlaceType } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; placeTypeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, placeTypeId: placeTypeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeTypeId = parseId(placeTypeIdRaw);
  if (coworkingId == null || placeTypeId == null) return NextResponse.json({ message: 'Invalid place type path.' }, { status: 400 });
  let payload: { name?: string; active?: boolean };
  try {
    payload = await request.json() as { name?: string; active?: boolean };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await updatePlaceType(session.token, coworkingId, placeTypeId, {
      name: payload.name.trim(),
      active: payload.active,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update place type.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; placeTypeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, placeTypeId: placeTypeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeTypeId = parseId(placeTypeIdRaw);
  if (coworkingId == null || placeTypeId == null) return NextResponse.json({ message: 'Invalid place type path.' }, { status: 400 });
  try {
    await archivePlaceType(session.token, coworkingId, placeTypeId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive place type.' }, { status: 500 });
  }
}
