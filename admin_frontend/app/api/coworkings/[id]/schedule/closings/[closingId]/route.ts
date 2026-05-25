import { NextResponse } from 'next/server';
import { archivePlaceClosing, BackendRequestError } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; closingId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const p = await params;
  const coworkingId = parseId(p.id);
  const closingId = parseId(p.closingId);
  if (coworkingId == null || closingId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });
  try {
    await archivePlaceClosing(session.token, coworkingId, closingId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось архивировать закрытие места.' }, { status: 500 });
  }
}
