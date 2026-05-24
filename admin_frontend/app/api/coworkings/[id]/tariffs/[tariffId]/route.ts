import { NextResponse } from 'next/server';
import { archiveTariff, BackendRequestError, getTariff, updateTariff } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string; tariffId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = parseId(id);
  const tariffId = parseId(tariffIdRaw);
  if (coworkingId == null || tariffId == null) return NextResponse.json({ message: 'Некорректный идентификатор тарифа.' }, { status: 400 });
  try {
    return NextResponse.json(await getTariff(session.token, coworkingId, tariffId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось загрузить тариф.' }, { status: 500 });
  }
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string; tariffId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = parseId(id);
  const tariffId = parseId(tariffIdRaw);
  if (coworkingId == null || tariffId == null) return NextResponse.json({ message: 'Некорректный идентификатор тарифа.' }, { status: 400 });
  let payload: Record<string, unknown>;
  try {
    payload = await request.json() as Record<string, unknown>;
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (typeof payload.name !== 'string' || !payload.name.trim()) return NextResponse.json({ message: 'Укажите название.' }, { status: 400 });
  try {
    return NextResponse.json(await updateTariff(session.token, coworkingId, tariffId, {
      name: payload.name.trim(),
      pricePerDay: Number(payload.pricePerDay ?? 0),
      fullRefundHoursBefore: Number(payload.fullRefundHoursBefore ?? 0),
      lateCancellationRefundPercent: Number(payload.lateCancellationRefundPercent ?? 0),
      cancellationCompensationCoefficient: Number(payload.cancellationCompensationCoefficient ?? 0),
      dayClosureCompensationCoefficient: Number(payload.dayClosureCompensationCoefficient ?? 0),
      membershipBlockCompensationCoefficient: Number(payload.membershipBlockCompensationCoefficient ?? 0),
      active: typeof payload.active === 'boolean' ? payload.active : undefined,
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось обновить тариф.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; tariffId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const { id, tariffId: tariffIdRaw } = await params;
  const coworkingId = parseId(id);
  const tariffId = parseId(tariffIdRaw);
  if (coworkingId == null || tariffId == null) return NextResponse.json({ message: 'Некорректный идентификатор тарифа.' }, { status: 400 });
  try {
    await archiveTariff(session.token, coworkingId, tariffId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось архивировать тариф.' }, { status: 500 });
  }
}
