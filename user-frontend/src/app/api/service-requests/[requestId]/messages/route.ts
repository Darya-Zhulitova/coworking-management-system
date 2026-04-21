import { NextResponse } from 'next/server';
import { BackendRequestError, createServiceRequestMessage } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

type CreateMessagePayload = {
  coworkingId?: number;
  text?: string;
};

export async function POST(request: Request, { params }: { params: Promise<{ requestId: string }> }) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const requestId = Number((await params).requestId);
  if (!Number.isInteger(requestId) || requestId <= 0) {
    return NextResponse.json({ message: 'Invalid request id.' }, { status: 400 });
  }

  let payload: CreateMessagePayload;
  try {
    payload = (await request.json()) as CreateMessagePayload;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!Number.isInteger(payload.coworkingId) || Number(payload.coworkingId) <= 0) {
    return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  }
  if (!payload.text?.trim()) {
    return NextResponse.json({ message: 'Message text is required.' }, { status: 400 });
  }

  try {
    const created = await createServiceRequestMessage({
      coworkingId: Number(payload.coworkingId),
      requestId,
      text: payload.text.trim(),
    }, session.token);
    return NextResponse.json(created, { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to send message.' }, { status: 500 });
  }
}
