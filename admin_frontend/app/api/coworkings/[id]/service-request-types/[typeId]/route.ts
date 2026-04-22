import { NextResponse } from 'next/server';
import { archiveServiceRequestType, BackendRequestError, updateServiceRequestType } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; typeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, typeId } = await params;
  const coworkingId = parseId(id);
  const serviceRequestTypeId = parseId(typeId);
  if (coworkingId == null || serviceRequestTypeId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  const payload = await request.json().catch(() => null) as { name?: string; cost?: number; active?: boolean } | null;
  if (!payload || !payload.name?.trim()) return NextResponse.json({ message: 'Service request type name is required.' }, { status: 400 });
  const cost = payload?.cost;
  if (!Number.isInteger(cost) || cost < 0) return NextResponse.json({ message: 'Service request type cost must be a non-negative integer.' }, { status: 400 });
  try {
    return NextResponse.json(await updateServiceRequestType(session.token, coworkingId, serviceRequestTypeId, {
      name: payload.name.trim(),
      cost: cost,
      active: payload.active,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update service request type.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; typeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, typeId } = await params;
  const coworkingId = parseId(id);
  const serviceRequestTypeId = parseId(typeId);
  if (coworkingId == null || serviceRequestTypeId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  try {
    await archiveServiceRequestType(session.token, coworkingId, serviceRequestTypeId);
    return NextResponse.json({ success: true });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive service request type.' }, { status: 500 });
  }
}
