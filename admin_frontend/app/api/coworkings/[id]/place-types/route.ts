import { NextResponse } from 'next/server';
import { BackendRequestError, createPlaceType, getPlaceTypes } from '@/lib/api/backend';
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
    return NextResponse.json(await getPlaceTypes(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load place types.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: { code?: string; name?: string; description?: string };
  try { payload = await request.json() as { code?: string; name?: string; description?: string }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.code?.trim() || !payload.name?.trim()) return NextResponse.json({ message: 'Code and name are required.' }, { status: 400 });
  try {
    return NextResponse.json(await createPlaceType(session.token, coworkingId, {
      code: payload.code.trim(),
      name: payload.name.trim(),
      description: payload.description?.trim(),
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create place type.' }, { status: 500 });
  }
}
