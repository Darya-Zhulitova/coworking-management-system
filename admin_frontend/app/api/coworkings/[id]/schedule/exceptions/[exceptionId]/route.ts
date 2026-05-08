import { NextResponse } from 'next/server';
import { archiveCoworkingScheduleException, BackendRequestError } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; exceptionId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const p = await params;
  const coworkingId = parseId(p.id);
  const exceptionId = parseId(p.exceptionId);
  if (coworkingId == null || exceptionId == null) return NextResponse.json({ message: 'Invalid id.' }, { status: 400 });
  try {
    await archiveCoworkingScheduleException(session.token, coworkingId, exceptionId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive schedule exception.' }, { status: 500 });
  }
}
