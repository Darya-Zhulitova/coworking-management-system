import { NextResponse } from 'next/server';
import { BackendRequestError, createCoworking, getCoworkings } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

export async function GET(request: Request) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const url = new URL(request.url);
  const archived = url.searchParams.get('archived') === 'true';
  try {
    return NextResponse.json(await getCoworkings(session.token, archived));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load coworkings.' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  let payload: { name?: string };
  try { payload = await request.json() as { name?: string }; } catch { return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 }); }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Coworking name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await createCoworking(session.token, { name: payload.name.trim() }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create coworking.' }, { status: 500 });
  }
}
