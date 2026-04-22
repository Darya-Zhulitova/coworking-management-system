import { NextResponse } from 'next/server';
import { BackendRequestError, createCoworking, getCoworkings } from '@/lib/api/backend';
import { getAdminSession } from '@/lib/auth/session';

export async function GET(request: Request) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  const url = new URL(request.url);
  const archived = url.searchParams.get('archived') === 'true';
  try {
    return NextResponse.json(await getCoworkings(session.token, archived));
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to load coworkings.' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });
  let payload: {
    name?: string;
    description?: string;
    address?: string;
    workingHoursLabel?: string;
    heroTitle?: string;
    heroText?: string;
    imageUrls?: string[];
    autoApproveMembership?: boolean
  };
  try {
    payload = await request.json() as {
      name?: string;
      description?: string;
      address?: string;
      workingHoursLabel?: string;
      heroTitle?: string;
      heroText?: string;
      imageUrls?: string[];
      autoApproveMembership?: boolean
    };
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }
  if (!payload.name?.trim()) return NextResponse.json({ message: 'Coworking name is required.' }, { status: 400 });
  if (!payload.description?.trim()) return NextResponse.json({ message: 'Coworking description is required.' }, { status: 400 });
  if (!payload.address?.trim()) return NextResponse.json({ message: 'Coworking address is required.' }, { status: 400 });
  if (!payload.workingHoursLabel?.trim()) return NextResponse.json({ message: 'Coworking working hours are required.' }, { status: 400 });
  try {
    return NextResponse.json(await createCoworking(session.token, {
      name: payload.name.trim(),
      description: payload.description.trim(),
      address: payload.address.trim(),
      workingHoursLabel: payload.workingHoursLabel.trim(),
      heroTitle: payload.heroTitle?.trim(),
      heroText: payload.heroText?.trim(),
      imageUrls: (payload.imageUrls ?? []).map((item) => item.trim()).filter(Boolean),
      autoApproveMembership: payload.autoApproveMembership
    }), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    return NextResponse.json({ message: 'Unable to create coworking.' }, { status: 500 });
  }
}
