import { NextResponse } from 'next/server';
import { BackendRequestError, createTariff, getTariffs } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  try {
    return NextResponse.json(await getTariffs(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось загрузить тарифы.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  let payload: Record<string, unknown>;
  try {
    payload = await request.json() as Record<string, unknown>;
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (typeof payload.name !== 'string' || !payload.name.trim()) return NextResponse.json({ message: 'Укажите название.' }, { status: 400 });
  try {
    return NextResponse.json(await createTariff(session.token, coworkingId, {
      name: payload.name.trim(),
      pricePerDay: Number(payload.pricePerDay ?? 0),
      fullRefundHoursBefore: Number(payload.fullRefundHoursBefore ?? 0),
      lateCancellationRefundPercent: Number(payload.lateCancellationRefundPercent ?? 0),
      cancellationCompensationCoefficient: Number(payload.cancellationCompensationCoefficient ?? 0),
      dayClosureCompensationCoefficient: Number(payload.dayClosureCompensationCoefficient ?? 0),
      membershipBlockCompensationCoefficient: Number(payload.membershipBlockCompensationCoefficient ?? 0),
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось создать тариф.' }, { status: 500 });
  }
}
