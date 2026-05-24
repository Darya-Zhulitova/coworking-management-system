import { NextResponse } from 'next/server';
import { BackendRequestError, commitDeactivatePlace } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);
  if (coworkingId == null || parsedPlaceId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  try {
    const payload = await request.json();
    if (!payload || typeof payload !== 'object' || Array.isArray(payload) || typeof (payload as {
      impactHash?: unknown
    }).impactHash !== 'string') {
      return NextResponse.json({ message: 'Сначала выполните предпросмотр изменений, затем подтвердите действие.' }, { status: 400 });
    }
    return NextResponse.json(await commitDeactivatePlace(session.token, coworkingId, parsedPlaceId, {
      impactHash: (payload as {
        impactHash: string
      }).impactHash
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось подтвердить деактивацию места.' }, { status: 500 });
  }
}
