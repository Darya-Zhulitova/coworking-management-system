import { registerUser } from '@/lib/api/backend';
import { badRequest, handleApiError, okJson, readJsonBody } from '@/lib/api/route-helpers';
import { setUserSession } from '@/lib/auth/session';
import type { UserRegisterRequest } from '@/types/auth';

export async function POST(request: Request) {
  const body = await readJsonBody<UserRegisterRequest>(request);
  if (!body.ok) return body.response;

  const payload = body.value;
  if (!payload.email?.trim() || !payload.password || !payload.name?.trim()) {
    return badRequest('Name, email and password are required.');
  }

  try {
    const authResponse = await registerUser({
      email: payload.email.trim(),
      password: payload.password,
      name: payload.name.trim(),
      description: payload.description?.trim() || undefined,
    });
    await setUserSession(authResponse);
    return okJson({ userId: authResponse.userId });
  } catch (error) {
    return handleApiError(error, 'Unable to register.');
  }
}
