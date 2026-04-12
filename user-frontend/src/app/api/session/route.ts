import { NextResponse } from 'next/server';
import { getUserSession } from '@/lib/auth/session';

export async function GET() {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  return NextResponse.json({ userId: session.userId, isAuthenticated: true });
}
