import { NextResponse } from 'next/server';
import { BackendRequestError, getAppContext } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

export async function GET(request: Request) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const url = new URL(request.url);
  const coworkingIdParam = url.searchParams.get('coworkingId');
  const coworkingId = coworkingIdParam == null ? undefined : Number(coworkingIdParam);
  if (coworkingIdParam != null && !Number.isInteger(coworkingId)) {
    return NextResponse.json({ message: 'Invalid coworkingId.' }, { status: 400 });
  }

  try {
    return NextResponse.json(await getAppContext(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load context.' }, { status: 500 });
  }
}
