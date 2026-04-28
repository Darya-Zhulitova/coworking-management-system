import { loginUser } from '@/lib/api/backend';
import { badRequest, handleApiError, okJson, readJsonBody } from '@/lib/api/route-helpers';
import { setUserSession } from '@/lib/auth/session';
import type { UserLoginRequest } from '@/types/auth';

export async function POST(request: Request) {
  const body = await readJsonBody<UserLoginRequest>(request);
  if (!body.ok) return body.response;

  const payload = body.value;
  if (!payload.email?.trim() || !payload.password) {
    return badRequest('Email and password are required.');
  }

  try {
    const loginResponse = await loginUser({ email: payload.email.trim(), password: payload.password });
    await setUserSession(loginResponse);
    return okJson({ userId: loginResponse.userId });
  } catch (error) {
    return handleApiError(error, 'Unable to sign in.');
  }
}
