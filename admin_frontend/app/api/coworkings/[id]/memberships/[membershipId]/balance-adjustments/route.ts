import { NextResponse } from 'next/server';
import { adjustMembershipBalance, BackendRequestError } from '@/lib/api/backend';
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
  const payload = await request.json().catch(() => null) as { amountMinorUnits?: unknown; comment?: unknown } | null;
  const amountMinorUnits = typeof payload?.amountMinorUnits === 'number' && Number.isInteger(payload.amountMinorUnits)
    ? payload.amountMinorUnits
    : null;
  if (amountMinorUnits == null || amountMinorUnits === 0) {
    return NextResponse.json({ message: 'Сумма корректировки должна быть целым числом и не должна быть равна нулю.' }, { status: 400 });
  }
  const comment = typeof payload?.comment === 'string' ? payload.comment.trim() : undefined;
  try {
    return NextResponse.json(await adjustMembershipBalance(session.token, coworkingId, parsedMembershipId, {
      amountMinorUnits,
      comment: comment || null,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось скорректировать баланс.' }, { status: 500 });
  }
}
