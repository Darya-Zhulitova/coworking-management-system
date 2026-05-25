import { NextResponse } from 'next/server';
import { BackendRequestError, previewMembershipBlock } from '@/lib/api/backend';
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
  const parsedMembershipId = parseId(membershipId);
  if (coworkingId == null || parsedMembershipId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  try {
    return NextResponse.json(await previewMembershipBlock(session.token, coworkingId, parsedMembershipId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось подготовить предпросмотр блокировки пользователя.' }, { status: 500 });
  }
}
