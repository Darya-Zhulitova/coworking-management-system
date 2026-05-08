import { NextResponse } from 'next/server';
import { BackendRequestError, createFloor, getFloors } from '@/lib/api/backend';
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
    return NextResponse.json(await getFloors(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load floors.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: { name?: string; imageFileId?: string };
  try {
    payload = await request.json() as { name?: string; imageFileId?: string };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await createFloor(session.token, coworkingId, {
      name: payload.name.trim(),
      imageFileId: payload.imageFileId?.trim(),
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create floor.' }, { status: 500 });
  }
}
