import { NextResponse } from 'next/server';
import { activatePlace, BackendRequestError } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(_request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);
  if (coworkingId == null || parsedPlaceId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  try {
    return NextResponse.json(await activatePlace(session.token, coworkingId, parsedPlaceId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось активировать место.' }, { status: 500 });
  }
}
