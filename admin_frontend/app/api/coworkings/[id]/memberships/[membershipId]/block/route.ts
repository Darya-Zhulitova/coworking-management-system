import { NextResponse } from 'next/server';
import { BackendRequestError, blockMembership } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string; membershipId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, membershipId } = await params;
  const coworkingId = parseId(id);
  const parsedMembershipId = parseId(membershipId);
  if (coworkingId == null || parsedMembershipId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  const payload = await request.json().catch(() => null) as { impactHash?: unknown } | null;
  const impactHash = typeof payload?.impactHash === 'string' ? payload.impactHash.trim() : '';
  if (!impactHash) return NextResponse.json({ message: 'Сначала выполните предпросмотр изменений, затем подтвердите действие.' }, { status: 400 });
  try {
    return NextResponse.json(await blockMembership(session.token, coworkingId, parsedMembershipId, { impactHash }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось заблокировать пользователя.' }, { status: 500 });
  }
}
