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
  let payload: { name?: string; tariffId?: number };
  try {
    payload = await request.json() as { name?: string; tariffId?: number };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim() || !payload.tariffId) return NextResponse.json({ message: 'Name and tariff are required.' }, { status: 400 });
  try {
    return NextResponse.json(await createPlaceType(session.token, coworkingId, {
      name: payload.name.trim(),
      tariffId: payload.tariffId,
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create place type.' }, { status: 500 });
  }
}
