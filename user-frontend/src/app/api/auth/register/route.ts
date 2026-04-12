import { NextResponse } from 'next/server';
import { BackendRequestError, registerUser } from '@/lib/api/backend';
import { setUserSession } from '@/lib/auth/session';
import type { UserRegisterRequest } from '@/types/auth';

export async function POST(request: Request) {
  let payload: UserRegisterRequest;
  try {
    payload = (await request.json()) as UserRegisterRequest;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!payload.email?.trim() || !payload.password || !payload.name?.trim()) {
    return NextResponse.json({ message: 'Name, email and password are required.' }, { status: 400 });
  }

  try {
    const authResponse = await registerUser({
      email: payload.email.trim(),
      password: payload.password,
      name: payload.name.trim(),
      description: payload.description?.trim() || undefined,
    });
    await setUserSession(authResponse);
    return NextResponse.json({ userId: authResponse.userId });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to register.' }, { status: 500 });
  }
}
