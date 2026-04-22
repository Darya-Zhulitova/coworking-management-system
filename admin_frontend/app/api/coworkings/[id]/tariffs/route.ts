import { NextResponse } from 'next/server';
import { BackendRequestError, createTariff, getTariffs } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

function normalizeDiscountRules(payload: unknown): Array<{ thresholdQuantity: number; discountPercent: number }> {
  if (!Array.isArray(payload)) return [];
  return payload
    .map((item) => {
      if (!item || typeof item !== 'object') return null;
      const source = item as Record<string, unknown>;
      return {
        thresholdQuantity: Number(source.thresholdQuantity ?? 0),
        discountPercent: Number(source.discountPercent ?? 0),
      };
    })
    .filter((item): item is { thresholdQuantity: number; discountPercent: number } => item !== null);
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  try {
    return NextResponse.json(await getTariffs(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load tariffs.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  let payload: Record<string, unknown>;
  try {
    payload = await request.json() as Record<string, unknown>;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (typeof payload.name !== 'string' || !payload.name.trim()) return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await createTariff(session.token, coworkingId, {
      name: payload.name.trim(),
      pricePerDay: Number(payload.pricePerDay ?? 0),
      minBookingDays: Number(payload.minBookingDays ?? 1),
      fullRefundHoursBefore: Number(payload.fullRefundHoursBefore ?? 0),
      lateCancellationRefundPercent: Number(payload.lateCancellationRefundPercent ?? 0),
      cancellationCompensationCoefficient: Number(payload.cancellationCompensationCoefficient ?? 0),
      dayClosureCompensationCoefficient: Number(payload.dayClosureCompensationCoefficient ?? 0),
      membershipBlockCompensationCoefficient: Number(payload.membershipBlockCompensationCoefficient ?? 0),
      discountRules: normalizeDiscountRules(payload.discountRules),
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create tariff.' }, { status: 500 });
  }
}
