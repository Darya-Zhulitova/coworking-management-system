import { NextResponse } from 'next/server';
import { BackendRequestError, createPlace, getPlaces } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  const url = new URL(request.url);
  const placeTypeIdRaw = url.searchParams.get('placeTypeId');
  const placeTypeId = placeTypeIdRaw == null ? undefined : parseId(placeTypeIdRaw) ?? undefined;
  try {
    return NextResponse.json(await getPlaces(session.token, coworkingId, placeTypeId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось загрузить места.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  let payload: {
    name?: string;
    floorId?: number;
    placeTypeId?: number;
    locX?: number;
    locY?: number;
    imageFileId?: string | null;
    amenities?: string[]
  };
  try {
    payload = await request.json() as {
      name?: string;
      floorId?: number;
      placeTypeId?: number;
      locX?: number;
      locY?: number;
      imageFileId?: string | null;
      amenities?: string[]
    };
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (!payload.name?.trim() || !payload.floorId || !payload.placeTypeId) return NextResponse.json({ message: 'Укажите название, этаж и тип места.' }, { status: 400 });
  try {
    return NextResponse.json(await createPlace(session.token, coworkingId, {
      name: payload.name.trim(),
      floorId: payload.floorId,
      placeTypeId: payload.placeTypeId,
      locX: payload.locX,
      locY: payload.locY,
      imageFileId: payload.imageFileId,
      amenities: payload.amenities,
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось создать место.' }, { status: 500 });
  }
}
