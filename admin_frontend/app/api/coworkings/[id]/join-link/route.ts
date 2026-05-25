import { NextResponse } from 'next/server';
import {
  BackendRequestError,
  deleteCoworkingJoinLink,
  generateCoworkingJoinLink,
  getCoworkingJoinLink
} from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

async function requireContext(params: Promise<{ id: string }>) {
  const session = await getAdminSession();
  if (!session) {
    return { error: NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 }) };
  }
  const id = parseId((await params).id);
  if (id == null) {
    return { error: NextResponse.json({ message: 'Некорректный идентификатор коворкинга.' }, { status: 400 }) };
  }
  return { session, id };
}

function handleError(error: unknown, fallbackMessage: string) {
  if (error instanceof BackendRequestError) {
    return NextResponse.json({ message: error.message }, { status: error.status || 500 });
  }
  return NextResponse.json({ message: fallbackMessage }, { status: 500 });
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireContext(params);
  if (context.error) return context.error;
  try {
    return NextResponse.json(await getCoworkingJoinLink(context.session.token, context.id));
  } catch (error) {
    return handleError(error, 'Не удалось загрузить ссылку приглашения в коворкинг.');
  }
}

export async function POST(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireContext(params);
  if (context.error) return context.error;
  try {
    return NextResponse.json(await generateCoworkingJoinLink(context.session.token, context.id));
  } catch (error) {
    return handleError(error, 'Не удалось создать ссылку приглашения в коворкинг.');
  }
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireContext(params);
  if (context.error) return context.error;
  try {
    return NextResponse.json(await deleteCoworkingJoinLink(context.session.token, context.id));
  } catch (error) {
    return handleError(error, 'Не удалось удалить ссылку приглашения в коворкинг.');
  }
}
