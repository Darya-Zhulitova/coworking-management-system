import { NextResponse } from 'next/server';
import { BackendRequestError, uploadFloorPlan } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string; floorId: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const { id, floorId: floorIdRaw } = await params;
  const coworkingId = parseId(id);
  const floorId = parseId(floorIdRaw);
  if (coworkingId == null || floorId == null) {
    return NextResponse.json({ message: 'Invalid floor path.' }, { status: 400 });
  }

  const formData = await request.formData().catch(() => null);
  const file = formData?.get('file');
  if (!(file instanceof File) || file.size === 0) {
    return NextResponse.json({ message: 'Floor plan image is required.' }, { status: 400 });
  }

  const backendFormData = new FormData();
  backendFormData.set('file', file);
  try {
    return NextResponse.json(await uploadFloorPlan(session.token, coworkingId, floorId, backendFormData));
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to upload floor plan.' }, { status: 500 });
  }
}
