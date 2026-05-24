import { NextResponse } from 'next/server';
import { assignCoworkingRole, BackendRequestError, getCoworkingAccessList } from '@/lib/api/backend';
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
    return NextResponse.json(await getCoworkingAccessList(session.token, id));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось загрузить доступы сотрудников.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  let payload: { email?: string; roleId?: number };
  try {
    payload = await request.json() as { email?: string; roleId?: number };
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (!payload.email?.trim() || !payload.roleId) return NextResponse.json({ message: 'Укажите email и роль.' }, { status: 400 });
  try {
    return NextResponse.json(await assignCoworkingRole(session.token, id, {
      email: payload.email.trim(),
      roleId: payload.roleId
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось назначить роль.' }, { status: 500 });
  }
}
