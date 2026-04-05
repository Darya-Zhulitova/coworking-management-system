import { NextResponse } from 'next/server';
import { BackendRequestError, deactivateTenantAccess, updateTenantRole } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; accessId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, accessId } = await params;
  const coworkingId = parseId(id);
  const parsedAccessId = parseId(accessId);
  if (coworkingId == null || parsedAccessId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  let payload: { role?: 'MANAGER' | 'STAFF_SUPPORT'; active?: boolean };
  try { payload = await request.json() as { role?: 'MANAGER' | 'STAFF_SUPPORT'; active?: boolean }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.role || typeof payload.active !== 'boolean') return NextResponse.json({ message: 'Role and active flag are required.' }, { status: 400 });
  try {
    return NextResponse.json(await updateTenantRole(session.token, coworkingId, parsedAccessId, { role: payload.role, active: payload.active }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update tenant role.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; accessId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, accessId } = await params;
  const coworkingId = parseId(id);
  const parsedAccessId = parseId(accessId);
  if (coworkingId == null || parsedAccessId == null) return NextResponse.json({ message: 'Invalid identifiers.' }, { status: 400 });
  try {
    await deactivateTenantAccess(session.token, coworkingId, parsedAccessId);
    return NextResponse.json({ success: true });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to deactivate tenant access.' }, { status: 500 });
  }
}
