import { NextResponse } from 'next/server';
import { BackendRequestError, getCoworkingDashboard } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  try {
    return NextResponse.json(await getCoworkingDashboard(session.token, id));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось загрузить сводку.' }, { status: 500 });
  }
}
