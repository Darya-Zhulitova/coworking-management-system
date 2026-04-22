import { NextResponse } from 'next/server';
import { BackendRequestError, createServiceRequestMessage, getServiceRequestMessages } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

type CreatePayload = { text?: string };

export async function GET(_request: Request, { params }: {
  params: Promise<{ id: string; serviceRequestId: string }>
}) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const { id, serviceRequestId } = await params;
  const coworkingId = parseId(id);
  const parsedRequestId = parseId(serviceRequestId);
  if (coworkingId == null || parsedRequestId == null) return NextResponse.json({ message: 'Invalid id.' }, { status: 400 });

  try {
    return NextResponse.json(await getServiceRequestMessages(session.token, coworkingId, parsedRequestId));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load service request messages.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: {
  params: Promise<{ id: string; serviceRequestId: string }>
}) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const { id, serviceRequestId } = await params;
  const coworkingId = parseId(id);
  const parsedRequestId = parseId(serviceRequestId);
  if (coworkingId == null || parsedRequestId == null) return NextResponse.json({ message: 'Invalid id.' }, { status: 400 });

  let payload: CreatePayload;
  try {
    payload = (await request.json()) as CreatePayload;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!payload.text?.trim()) {
    return NextResponse.json({ message: 'Message text is required.' }, { status: 400 });
  }

  try {
    return NextResponse.json(await createServiceRequestMessage(session.token, coworkingId, parsedRequestId, payload.text.trim()), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to send service request message.' }, { status: 500 });
  }
}
