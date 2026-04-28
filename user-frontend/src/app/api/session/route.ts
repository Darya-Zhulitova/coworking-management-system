import { okJson, requireApiSession } from '@/lib/api/route-helpers';

export async function GET() {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  return okJson({ userId: session.value.userId, isAuthenticated: true });
}
