import { createBookingsFromCart } from '@/lib/api/backend';
import {
  type CartPayload,
  createdJson,
  handleApiError,
  normalizeCartPayload,
  readJsonBody,
  requireApiSession,
} from '@/lib/api/route-helpers';

export async function POST(request: Request) {
  const session = await requireApiSession();
  if (!session.ok) return session.response;

  const body = await readJsonBody<CartPayload>(request);
  if (!body.ok) return body.response;

  const cart = normalizeCartPayload(body.value);
  if (!cart.ok) return cart.response;

  try {
    return createdJson(await createBookingsFromCart(cart.value, session.value.token));
  } catch (error) {
    return handleApiError(error, 'Unable to create bookings from cart.');
  }
}
