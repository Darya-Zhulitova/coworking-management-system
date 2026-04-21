import { NextResponse } from 'next/server';
import { BackendRequestError, getCoworking, getCurrentUser } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';
import type { CoworkingShellContext } from '@/lib/types';

function parseCoworkingId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ coworkingId: string }> }) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  const coworkingId = parseCoworkingId((await params).coworkingId);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });

  try {
    const [user, coworking] = await Promise.all([
      getCurrentUser(session.token),
      getCoworking(coworkingId, session.token),
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

    return NextResponse.json(context);
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to load coworking context.' }, { status: 500 });
  }
}
