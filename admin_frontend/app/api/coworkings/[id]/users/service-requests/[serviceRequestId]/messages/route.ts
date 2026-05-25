import { NextResponse } from 'next/server';
import { BackendRequestError, createServiceRequestMessage, getServiceRequestMessages } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function GET(_request: Request, { params }: {
  params: Promise<{ id: string; serviceRequestId: string }>
}) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });

  const { id, serviceRequestId } = await params;
  const coworkingId = parseId(id);
  const parsedRequestId = parseId(serviceRequestId);
  if (coworkingId == null || parsedRequestId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });

  try {
    return NextResponse.json(await getServiceRequestMessages(session.token, coworkingId, parsedRequestId));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Не удалось загрузить сообщения сервисной заявки.' }, { status: 500 });
  }
}

export async function POST(request: Request, { params }: {
  params: Promise<{ id: string; serviceRequestId: string }>
}) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });

  const { id, serviceRequestId } = await params;
  const coworkingId = parseId(id);
  const parsedRequestId = parseId(serviceRequestId);
  if (coworkingId == null || parsedRequestId == null) return NextResponse.json({ message: 'Некорректный идентификатор.' }, { status: 400 });

  const formData = await request.formData();
  const text = typeof formData.get('text') === 'string' ? String(formData.get('text')).trim() : '';
  const file = formData.get('file');
  if (!text && !(file instanceof File && file.size > 0)) {
    return NextResponse.json({ message: 'Добавьте текст сообщения или файл.' }, { status: 400 });
  }

  try {
    return NextResponse.json(await createServiceRequestMessage(session.token, coworkingId, parsedRequestId, formData), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Не удалось отправить сообщение по сервисной заявке.' }, { status: 500 });
  }
}
