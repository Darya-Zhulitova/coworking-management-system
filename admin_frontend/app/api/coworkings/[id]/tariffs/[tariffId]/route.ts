import { NextResponse } from 'next/server';
import { archiveTariff, BackendRequestError, getTariff, updateTariff } from '@/lib/api/backend';
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

export async function GET(_request: Request, { params }: { params: Promise<{ id: string; tariffId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = parseId(id);
  const tariffId = parseId(tariffIdRaw);
  if (coworkingId == null || tariffId == null) return NextResponse.json({ message: 'Invalid tariff path.' }, { status: 400 });
  try {
    return NextResponse.json(await getTariff(session.token, coworkingId, tariffId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load tariff.' }, { status: 500 });
  }
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; tariffId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = parseId(id);
  const tariffId = parseId(tariffIdRaw);
  if (coworkingId == null || tariffId == null) return NextResponse.json({ message: 'Invalid tariff path.' }, { status: 400 });
  let payload: Record<string, unknown>;
  try {
    payload = await request.json() as Record<string, unknown>;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (typeof payload.name !== 'string' || !payload.name.trim()) return NextResponse.json({ message: 'Name is required.' }, { status: 400 });
  try {
    return NextResponse.json(await updateTariff(session.token, coworkingId, tariffId, {
      name: payload.name.trim(),
      pricePerDay: Number(payload.pricePerDay ?? 0),
      minBookingDays: Number(payload.minBookingDays ?? 1),
      fullRefundHoursBefore: Number(payload.fullRefundHoursBefore ?? 0),
      lateCancellationRefundPercent: Number(payload.lateCancellationRefundPercent ?? 0),
      cancellationCompensationCoefficient: Number(payload.cancellationCompensationCoefficient ?? 0),
      dayClosureCompensationCoefficient: Number(payload.dayClosureCompensationCoefficient ?? 0),
      membershipBlockCompensationCoefficient: Number(payload.membershipBlockCompensationCoefficient ?? 0),
      discountRules: normalizeDiscountRules(payload.discountRules),
      active: typeof payload.active === 'boolean' ? payload.active : undefined,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update tariff.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; tariffId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = parseId(id);
  const tariffId = parseId(tariffIdRaw);
  if (coworkingId == null || tariffId == null) return NextResponse.json({ message: 'Invalid tariff path.' }, { status: 400 });
  try {
    await archiveTariff(session.token, coworkingId, tariffId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to archive tariff.' }, { status: 500 });
  }
}
