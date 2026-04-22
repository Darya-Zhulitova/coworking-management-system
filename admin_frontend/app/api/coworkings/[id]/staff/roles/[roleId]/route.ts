import { NextResponse } from 'next/server';
import { archiveCoworkingRole, BackendRequestError, updateCoworkingRoleDefinition } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; roleId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, roleId: roleIdRaw } = await params;
  const coworkingId = parseId(id);
  const roleId = parseId(roleIdRaw);
  if (coworkingId == null || roleId == null) return NextResponse.json({ message: 'Invalid role path.' }, { status: 400 });
  let payload: { name?: string; grants?: string[]; active?: boolean };
  try {
    payload = await request.json() as { name?: string; grants?: string[]; active?: boolean };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Role name is required.' }, { status: 400 });
  if (!Array.isArray(payload.grants) || payload.grants.length === 0) return NextResponse.json({ message: 'At least one grant is required.' }, { status: 400 });
  try {
    return NextResponse.json(await updateCoworkingRoleDefinition(session.token, coworkingId, roleId, {
      name: payload.name.trim(),
      grants: payload.grants,
      active: Boolean(payload.active),
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update coworking role.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; roleId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, roleId: roleIdRaw } = await params;
  const coworkingId = parseId(id);
  const roleId = parseId(roleIdRaw);
  if (coworkingId == null || roleId == null) return NextResponse.json({ message: 'Invalid role path.' }, { status: 400 });
  try {
    await archiveCoworkingRole(session.token, coworkingId, roleId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive coworking role.' }, { status: 500 });
  }
}
