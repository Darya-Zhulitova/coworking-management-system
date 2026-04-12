import { NextResponse } from 'next/server';
import { BackendRequestError, loginUser } from '@/lib/api/backend';
import { setUserSession } from '@/lib/auth/session';
import type { UserLoginRequest } from '@/types/auth';

export async function POST(request: Request) {
  let payload: UserLoginRequest;
  try {
    payload = (await request.json()) as UserLoginRequest;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!payload.email?.trim() || !payload.password) {
    return NextResponse.json({ message: 'Email and password are required.' }, { status: 400 });
  }

  try {
    const loginResponse = await loginUser({ email: payload.email.trim(), password: payload.password });
    await setUserSession(loginResponse);
    return NextResponse.json({ userId: loginResponse.userId });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to sign in.' }, { status: 500 });
  }
}
