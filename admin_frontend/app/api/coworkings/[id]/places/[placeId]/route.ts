import { NextResponse } from 'next/server';
import { archivePlace, BackendRequestError, updatePlace } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, placeId: placeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeId = parseId(placeIdRaw);
  if (coworkingId == null || placeId == null) return NextResponse.json({ message: 'Некорректный идентификатор места.' }, { status: 400 });
  let payload: {
    name?: string;
    locX?: number | null;
    locY?: number | null;
    imageFileId?: string | null;
    amenities?: string[];
    active?: boolean
  };
  try {
    payload = await request.json() as {
      name?: string;
      locX?: number | null;
      locY?: number | null;
      imageFileId?: string | null;
      amenities?: string[];
      active?: boolean
    };
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Укажите название.' }, { status: 400 });
  try {
    return NextResponse.json(await updatePlace(session.token, coworkingId, placeId, {
      name: payload.name.trim(),
      locX: payload.locX,
      locY: payload.locY,
      imageFileId: payload.imageFileId,
      amenities: payload.amenities,
      active: payload.active,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось обновить место.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, placeId: placeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeId = parseId(placeIdRaw);
  if (coworkingId == null || placeId == null) return NextResponse.json({ message: 'Некорректный идентификатор места.' }, { status: 400 });
  try {
    await archivePlace(session.token, coworkingId, placeId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось архивировать место.' }, { status: 500 });
  }
}
