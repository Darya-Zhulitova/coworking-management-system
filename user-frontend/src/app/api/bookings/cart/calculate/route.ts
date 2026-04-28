import { calculateBookingCart } from '@/lib/api/backend';
import {
  type CartPayload,
  handleApiError,
  normalizeCartPayload,
  okJson,
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
    return okJson(await calculateBookingCart(cart.value, session.value.token));
  } catch (error) {
    return handleApiError(error, 'Unable to calculate cart.');
  }
}
