import { NextResponse } from 'next/server';
import { archiveCoworking, BackendRequestError, getCoworking, updateCoworking } from '@/lib/api/backend';
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
    return NextResponse.json(await getCoworking(session.token, id));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load coworking.' }, { status: 500 });
  }
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: { name?: string; active?: boolean };
  try { payload = await request.json() as { name?: string; active?: boolean }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Coworking name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await updateCoworking(session.token, id, { name: payload.name.trim(), active: payload.active }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update coworking.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  try {
    await archiveCoworking(session.token, id);
    return NextResponse.json({ success: true });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive coworking.' }, { status: 500 });
  }
}
