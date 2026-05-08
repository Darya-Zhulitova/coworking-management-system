import { NextResponse } from 'next/server';
import { BackendRequestError, createServiceRequestType, getServiceRequestTypes } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  try {
    return NextResponse.json(await getServiceRequestTypes(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load service request types.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  const payload = await request.json().catch(() => null) as { name?: string; cost?: number } | null;
  if (!payload || !payload.name?.trim()) return NextResponse.json({ message: 'Service request type name is required.' }, { status: 400 });
  const cost = payload?.cost;
  if (!Number.isInteger(cost) || cost < 0) return NextResponse.json({ message: 'Service request type cost must be a non-negative integer.' }, { status: 400 });
  try {
    return NextResponse.json(await createServiceRequestType(session.token, coworkingId, {
      name: payload.name.trim(),
      cost: cost
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create service request type.' }, { status: 500 });
  }
}
