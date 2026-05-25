import { NextResponse } from 'next/server';
import { BackendRequestError, uploadPlacePhoto } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string; placeId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Необходимо войти в систему.' }, { status: 401 });

  const { id, placeId: placeIdRaw } = await params;
  const coworkingId = parseId(id);
  const placeId = parseId(placeIdRaw);
  if (coworkingId == null || placeId == null) {
    return NextResponse.json({ message: 'Некорректный идентификатор места.' }, { status: 400 });
  }

  const formData = await request.formData().catch(() => null);
  const file = formData?.get('file');
  if (!(file instanceof File) || file.size === 0) {
    return NextResponse.json({ message: 'Загрузите фотографию места.' }, { status: 400 });
  }

  const backendFormData = new FormData();
  backendFormData.set('file', file);
  try {
    return NextResponse.json(await uploadPlacePhoto(session.token, coworkingId, placeId, backendFormData));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Не удалось загрузить фотографию места.' }, { status: 500 });
  }
}
