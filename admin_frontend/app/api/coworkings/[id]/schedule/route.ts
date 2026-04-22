import { NextResponse } from 'next/server';
import { BackendRequestError, getCoworkingSchedule, updateCoworkingSchedule } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

function parseId(value: string): number | null {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

type SchedulePayload = {
  monday: boolean;
  tuesday: boolean;
  wednesday: boolean;
  thursday: boolean;
  friday: boolean;
  saturday: boolean;
  sunday: boolean;
};

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  try {
    return NextResponse.json(await getCoworkingSchedule(session.token, coworkingId));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load schedule.' }, { status: 500 });
  }
}

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const coworkingId = parseId((await params).id);
  if (coworkingId == null) return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  const payload = await request.json().catch(() => null) as Partial<SchedulePayload> | null;
  if (!payload
    || typeof payload.monday !== 'boolean'
    || typeof payload.tuesday !== 'boolean'
    || typeof payload.wednesday !== 'boolean'
    || typeof payload.thursday !== 'boolean'
    || typeof payload.friday !== 'boolean'
    || typeof payload.saturday !== 'boolean'
    || typeof payload.sunday !== 'boolean') {
    return NextResponse.json({ message: 'All weekday boolean values are required.' }, { status: 400 });
  }
  try {
    return NextResponse.json(await updateCoworkingSchedule(session.token, coworkingId, payload));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to update schedule.' }, { status: 500 });
  }
}
