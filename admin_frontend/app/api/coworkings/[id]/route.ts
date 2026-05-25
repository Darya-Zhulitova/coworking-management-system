import { NextResponse } from 'next/server';
import { archiveCoworking, BackendRequestError, getCoworking, updateCoworking } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  try {
    return NextResponse.json(await getCoworking(session.token, id));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось получить данные коворкинга.' }, { status: 500 });
  }
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  let payload: {
    name?: string;
    description?: string;
    address?: string;
    workingHoursLabel?: string;
    heroTitle?: string;
    heroText?: string;
    imageFileIds?: string[];
    active?: boolean;
    autoApproveMembership?: boolean;
    floorMapEnabled?: boolean
  };
  try {
    payload = await request.json() as {
      name?: string;
      description?: string;
      address?: string;
      workingHoursLabel?: string;
      heroTitle?: string;
      heroText?: string;
      imageFileIds?: string[];
      active?: boolean;
      autoApproveMembership?: boolean;
      floorMapEnabled?: boolean
    };
  } catch {
    return NextResponse.json({ message: 'Некорректное тело запроса.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Укажите название коворкинга.' }, { status: 400 });
  if (!payload.description?.trim()) return NextResponse.json({ message: 'Укажите описание коворкинга.' }, { status: 400 });
  if (!payload.address?.trim()) return NextResponse.json({ message: 'Укажите адрес коворкинга.' }, { status: 400 });
  if (!payload.workingHoursLabel?.trim()) return NextResponse.json({ message: 'Укажите часы работы коворкинга.' }, { status: 400 });
  try {
    return NextResponse.json(await updateCoworking(session.token, id, {
      name: payload.name.trim(),
      description: payload.description.trim(),
      address: payload.address.trim(),
      workingHoursLabel: payload.workingHoursLabel.trim(),
      heroTitle: payload.heroTitle?.trim(),
      heroText: payload.heroText?.trim(),
      imageFileIds: (payload.imageFileIds ?? []).map((item) => item.trim()).filter(Boolean),
      active: payload.active,
      autoApproveMembership: payload.autoApproveMembership,
      floorMapEnabled: payload.floorMapEnabled
    }));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось обновить коворкинг.' }, { status: 500 });
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  const id = parseId((await params).id);
  if (id == null) return NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 });
  try {
    await archiveCoworking(session.token, id);
    return NextResponse.json({ success: true });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Не удалось архивировать коворкинг.' }, { status: 500 });
  }
}
