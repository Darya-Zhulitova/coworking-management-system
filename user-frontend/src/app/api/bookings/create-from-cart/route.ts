import { NextResponse } from 'next/server';
import { BackendRequestError, createBookingsFromCart } from '@/lib/api/backend';
import { getUserSession } from '@/lib/auth/session';
import type { BookingCartItem } from '@/lib/types';

type CheckoutPayload = {
  coworkingId?: number;
  items?: BookingCartItem[];
};

export async function POST(request: Request) {
  const session = await getUserSession();
  if (!session) return NextResponse.json({ message: 'Unauthorized.' }, { status: 401 });

  let payload: CheckoutPayload;
  try {
    payload = (await request.json()) as CheckoutPayload;
  } catch {
    return NextResponse.json({ message: 'Invalid request body.' }, { status: 400 });
  }

  if (!Number.isInteger(payload.coworkingId) || Number(payload.coworkingId) <= 0) {
    return NextResponse.json({ message: 'Invalid coworking id.' }, { status: 400 });
  }
  if (!Array.isArray(payload.items) || payload.items.length === 0) {
    return NextResponse.json({ message: 'Cart must contain at least one item.' }, { status: 400 });
  }
  if (payload.items.some((item) => !Number.isInteger(item.placeId) || Number(item.placeId) <= 0 || !item.date)) {
    return NextResponse.json({ message: 'Cart items are invalid.' }, { status: 400 });
  }

  try {
    return NextResponse.json(await createBookingsFromCart({
      coworkingId: Number(payload.coworkingId),
      items: payload.items.map((item) => ({ placeId: Number(item.placeId), date: item.date })),
    }, session.token), { status: 201 });
  } catch (error) {
    if (error instanceof BackendRequestError) {
      return NextResponse.json({ message: error.message }, { status: error.status || 500 });
    }
    return NextResponse.json({ message: 'Unable to create bookings from cart.' }, { status: 500 });
  }
}
