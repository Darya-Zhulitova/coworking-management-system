import { NextResponse } from 'next/server';
import { approveMembership, BackendRequestError } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(_request: Request, { params }: { params: Promise<{ id: string; membershipId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, membershipId } = await params;
  const coworkingId = parseId(id);
  const parsedId = parseId(membershipId);
  if (coworkingId == null || parsedId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  try {
    await approveMembership(session.token, coworkingId, parsedId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось обновить заявку пользователя.' }, { status: 500 });
  }
}
