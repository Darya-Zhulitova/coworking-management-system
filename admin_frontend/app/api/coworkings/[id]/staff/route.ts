import { NextResponse } from 'next/server';
import { assignTenantRole, BackendRequestError, getTenantStaff } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  try {
    return NextResponse.json(await getTenantStaff(session.token, id));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load tenant staff.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: { email?: string; role?: 'MANAGER' | 'STAFF_SUPPORT' };
  try { payload = await request.json() as { email?: string; role?: 'MANAGER' | 'STAFF_SUPPORT' }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.email?.trim() || !payload.role) return NextResponse.json({ message: 'Email and role are required.' }, { status: 400 });
  try {
    return NextResponse.json(await assignTenantRole(session.token, id, { email: payload.email.trim(), role: payload.role }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to assign tenant role.' }, { status: 500 });
  }
}
