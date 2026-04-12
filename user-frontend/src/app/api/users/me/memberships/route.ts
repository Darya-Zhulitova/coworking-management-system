import { NextResponse } from 'next/server';
import { BackendRequestError, getCurrentUserMemberships } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';

export async function GET() {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  try {
    return NextResponse.json(await getCurrentUserMemberships(session.token));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load memberships.' }, { status: 500 });
  }
}
