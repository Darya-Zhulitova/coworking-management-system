import { NextResponse } from 'next/server';
import { BackendRequestError, getServiceRequestMessages } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

function parsePositiveId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export async function GET(_request: Request, { params }: {
  params: Promise<{ coworkingId: string; requestId: string }>
}) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const { coworkingId: rawCoworkingId, requestId: rawRequestId } = await params;
  const coworkingId = parsePositiveId(rawCoworkingId);
  const requestId = parsePositiveId(rawRequestId);
  if (coworkingId == null || requestId == null) return NextResponse.json({ message: 'Invalid request parameters.' }, { status: 400 });

  try {
    return NextResponse.json(await getServiceRequestMessages(coworkingId, requestId, session.token));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load request messages.' }, { status: 500 });
  }
}
