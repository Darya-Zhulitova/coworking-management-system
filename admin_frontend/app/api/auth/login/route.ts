import { NextResponse } from 'next/server';
import { BackendRequestError, loginAdmin } from '@/lib/api/backend';
import { setAdminSession } from '@/lib/auth/session';
import type { AdminLoginRequest } from '@/types/auth';

export async function POST(request: Request) {
  let payload: AdminLoginRequest;
  try {
    payload = (await request.json()) as AdminLoginRequest;
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }

  if (!payload.email?.trim() || !payload.password) {
    return NextResponse.json({ message: 'Укажите email и пароль.' }, { status: 400 });
  }

  try {
    const loginResponse = await loginAdmin({ email: payload.email.trim(), password: payload.password });
    await setAdminSession(loginResponse);
    return NextResponse.json({ adminId: loginResponse.adminId });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Не удалось войти в систему.' }, { status: 500 });
  }
}
