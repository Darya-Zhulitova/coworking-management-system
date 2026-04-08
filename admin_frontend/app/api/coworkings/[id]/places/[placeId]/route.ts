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
  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);
  if (coworkingId == null || parsedPlaceId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  let payload: { name?: string; placeTypeId?: number; active?: boolean };
  try { payload = await request.json() as { name?: string; placeTypeId?: number; active?: boolean }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await updatePlace(session.token, coworkingId, parsedPlaceId, {
      name: payload.name.trim(),
      placeTypeId: payload.placeTypeId,
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
  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);
  if (coworkingId == null || parsedPlaceId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  try {
    await archivePlace(session.token, coworkingId, parsedPlaceId);
    return NextResponse.json({ success: true });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive place.' }, { status: 500 });
  }
}
