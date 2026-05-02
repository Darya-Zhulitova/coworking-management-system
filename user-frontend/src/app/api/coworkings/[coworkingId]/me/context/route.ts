import { getCoworking, getCurrentUser } from '@/lib/api/backend';
import { handleApiError, okJson, parseCoworkingId, requireApiSession } from '@/lib/api/route-helpers';
import type { CoworkingShellContext } from '@/lib/types';

export async function GET(_request: Request, { params }: { params: Promise<{ coworkingId: string }> }) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const parsedCoworkingId = parseCoworkingId((await params).coworkingId);
  if (!parsedCoworkingId.ok) return parsedCoworkingId.response;

  try {
    const [user, coworking] = await Promise.all([
      getCurrentUser(session.value.token),
      getCoworking(parsedCoworkingId.value, session.value.token),
    ]);

    const context: CoworkingShellContext = {
      user,
      coworking,
      membership: {
        id: coworking.membershipId ?? null,
        status: coworking.membershipStatus ?? null,
        balanceMinorUnits: coworking.balanceMinorUnits ?? 0,
      },
    };

    return okJson(context);
  } catch (error) {
    return handleApiError(error, 'Unable to load coworking context.');
  }
}
