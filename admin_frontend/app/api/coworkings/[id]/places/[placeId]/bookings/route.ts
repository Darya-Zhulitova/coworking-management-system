import { NextResponse } from 'next/server';
import { BackendRequestError, commitPlaceClosing, getPlaceBookings, previewPlaceClosing } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });

  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);

  if (coworkingId == null || parsedPlaceId == null) {
    return NextResponse.json({ message: 'Некорректный идентификатор коворкинга или места.' }, { status: 400 });
  }

  try {
    return NextResponse.json(await getPlaceBookings(session.token, coworkingId, parsedPlaceId));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Не удалось загрузить бронирования места.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });

  const { id, placeId } = await params;
  const coworkingId = parseId(id);
  const parsedPlaceId = parseId(placeId);

  if (coworkingId == null || parsedPlaceId == null) {
    return NextResponse.json({ message: 'Некорректный идентификатор коворкинга или места.' }, { status: 400 });
  }

  const payload = await request.json().catch(() => null) as { bookingId?: unknown; date?: unknown } | null;
  const date = typeof payload?.date === 'string' ? payload.date.slice(0, 10) : '';
  const bookingId = typeof payload?.bookingId === 'number' && Number.isInteger(payload.bookingId) ? payload.bookingId : null;
  if (!date || bookingId == null) {
    return NextResponse.json({ message: 'Укажите бронирование и дату.' }, { status: 400 });
  }

  try {
    const closing = {
      placeId: parsedPlaceId,
      date,
      name: `Отмена брони #${bookingId}`,
    };
    const preview = await previewPlaceClosing(session.token, coworkingId, closing);
    const committed = await commitPlaceClosing(session.token, coworkingId, {
      ...closing,
      impactHash: preview.impactHash,
    });
    return NextResponse.json(committed);
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Не удалось отменить бронирование при закрытии места.' }, { status: 500 });
  }
}
