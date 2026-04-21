import { NextResponse } from 'next/server';
import { BackendRequestError, createServiceRequest } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

type CreateServiceRequestPayload = {
  coworkingId?: number;
  typeId?: number;
  name?: string;
};

export async function POST(request: Request) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  let payload: CreateServiceRequestPayload;
  try {
    payload = (await request.json()) as CreateServiceRequestPayload;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!Number.isInteger(payload.coworkingId) || Number(payload.coworkingId) <= 0) {
    return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  }
  if (!Number.isInteger(payload.typeId) || Number(payload.typeId) <= 0) {
    return NextResponse.json({ message: 'Invalid request type.' }, { status: 400 });
  }
  if (!payload.name?.trim()) {
    return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  }

  try {
    const created = await createServiceRequest({
      coworkingId: Number(payload.coworkingId),
      typeId: Number(payload.typeId),
      name: payload.name.trim(),
    }, session.token);
    return NextResponse.json(created, { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to create service request.' }, { status: 500 });
  }
}
