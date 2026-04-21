import { NextResponse } from 'next/server';
import { BackendRequestError, createPayRequest } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

type CreatePayRequestPayload = {
  coworkingId?: number;
  amount?: number;
  userComment?: string;
};

export async function POST(request: Request) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  let payload: CreatePayRequestPayload;
  try {
    payload = (await request.json()) as CreatePayRequestPayload;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!Number.isInteger(payload.coworkingId) || Number(payload.coworkingId) <= 0) {
    return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  }

  if (!Number.isInteger(payload.amount) || Number(payload.amount) === 0) {
    return NextResponse.json({ message: 'Amount must be a non-zero integer.' }, { status: 400 });
  }

  if (!payload.userComment?.trim()) {
    return NextResponse.json({ message: 'Comment is required.' }, { status: 400 });
  }

  try {
    const created = await createPayRequest({
      coworkingId: Number(payload.coworkingId),
      amount: Number(payload.amount),
      userComment: payload.userComment.trim(),
    }, session.token);
    return NextResponse.json(created, { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to create pay request.' }, { status: 500 });
  }
}
