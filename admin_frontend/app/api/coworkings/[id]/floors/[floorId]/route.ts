import { NextResponse } from 'next/server';
import { archiveFloor, BackendRequestError, updateFloor } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; floorId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, floorId: floorIdRaw } = await params;
  const coworkingId = parseId(id);
  const floorId = parseId(floorIdRaw);
  if (coworkingId == null || floorId == null) return NextResponse.json({ message: 'Некорректный идентификатор этажа.' }, { status: 400 });
  let payload: { name?: string; imageFileId?: string; active?: boolean };
  try {
    payload = await request.json() as { name?: string; imageFileId?: string; active?: boolean };
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Укажите название.' }, { status: 400 });
  try {
    return NextResponse.json(await updateFloor(session.token, coworkingId, floorId, {
      name: payload.name.trim(),
      imageFileId: payload.imageFileId?.trim(),
      active: payload.active,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось обновить этаж.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; floorId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, floorId: floorIdRaw } = await params;
  const coworkingId = parseId(id);
  const floorId = parseId(floorIdRaw);
  if (coworkingId == null || floorId == null) return NextResponse.json({ message: 'Некорректный идентификатор этажа.' }, { status: 400 });
  try {
    await archiveFloor(session.token, coworkingId, floorId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось архивировать этаж.' }, { status: 500 });
  }
}
