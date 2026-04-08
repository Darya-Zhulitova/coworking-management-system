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
  const { id, placeTypeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceTypeId = parseId(placeTypeId);
  if (coworkingId == null || parsedPlaceTypeId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  let payload: { code?: string; name?: string; description?: string; active?: boolean };
  try { payload = await request.json() as { code?: string; name?: string; description?: string; active?: boolean }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.code?.trim() || !payload.name?.trim()) return NextResponse.json({ message: 'Code and name are required.' }, { status: 400 });
  try {
    return NextResponse.json(await updatePlaceType(session.token, coworkingId, parsedPlaceTypeId, {
      code: payload.code.trim(),
      name: payload.name.trim(),
      description: payload.description?.trim(),
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
  const { id, placeTypeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceTypeId = parseId(placeTypeId);
  if (coworkingId == null || parsedPlaceTypeId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  try {
    await archivePlaceType(session.token, coworkingId, parsedPlaceTypeId);
    return NextResponse.json({ success: true });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive place type.' }, { status: 500 });
  }
}
