import { NextResponse } from 'next/server';
import { BackendRequestError, rejectPayRequest } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string; payRequestId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, payRequestId } = await params;
  const coworkingId = parseId(id);
  const parsedId = parseId(payRequestId);
  if (coworkingId == null || parsedId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  try {
    const body = await request.json().catch(() => ({})) as { comment?: unknown };
    const comment = typeof body.comment === 'string' ? body.comment.trim() : undefined;
    await rejectPayRequest(session.token, coworkingId, parsedId, comment && comment.length > 0 ? comment : undefined);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось обновить платежную заявку.' }, { status: 500 });
  }
}
