import { NextResponse } from 'next/server';
import { BackendRequestError, updateServiceRequestStatus } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(_request: Request, { params }: {
  params: Promise<{ id: string; serviceRequestId: string; status: string }>
}) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, serviceRequestId, status } = await params;
  const coworkingId = parseId(id);
  const parsedId = parseId(serviceRequestId);
  if (coworkingId == null || parsedId == null) return NextResponse.json({ message: 'Invalid id.' }, { status: 400 });
  try {
    await updateServiceRequestStatus(session.token, coworkingId, parsedId, status);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update service request.' }, { status: 500 });
  }
}
