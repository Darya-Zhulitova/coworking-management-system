import { NextResponse } from 'next/server';
import {
  BackendRequestError,
  createCoworkingScheduleException,
  getCoworkingScheduleExceptions
} from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  try {
    return NextResponse.json(await getCoworkingScheduleExceptions(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось загрузить исключения расписания.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  const payload = await request.json().catch(() => null) as {
    date?: string;
    type?: 'OPEN' | 'CLOSE';
    name?: string
  } | null;
  if (!payload?.date || !payload?.type || !payload?.name) return NextResponse.json({ message: 'Укажите дату, тип и название.' }, { status: 400 });
  try {
    return NextResponse.json(await createCoworkingScheduleException(session.token, coworkingId, {
      date: payload.date,
      type: payload.type,
      name: payload.name
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось создать исключение расписания.' }, { status: 500 });
  }
}
