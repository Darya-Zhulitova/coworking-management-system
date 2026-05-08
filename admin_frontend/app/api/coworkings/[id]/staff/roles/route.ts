import { NextResponse } from 'next/server';
import { BackendRequestError, createCoworkingRole, getCoworkingRoles } from '@/lib/api/backend';
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
    return NextResponse.json(await getCoworkingRoles(session.token, id));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load coworking roles.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: { name?: string; grants?: string[]; active?: boolean };
  try {
    payload = await request.json() as { name?: string; grants?: string[]; active?: boolean };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Role name is required.' }, { status: 400 });
  if (!Array.isArray(payload.grants) || payload.grants.length === 0) return NextResponse.json({ message: 'At least one grant is required.' }, { status: 400 });
  try {
    return NextResponse.json(await createCoworkingRole(session.token, id, {
      name: payload.name.trim(),
      grants: payload.grants,
      active: payload.active,
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create coworking role.' }, { status: 500 });
  }
}
