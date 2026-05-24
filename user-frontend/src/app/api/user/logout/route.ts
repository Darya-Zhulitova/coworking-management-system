import { okJson } from '@/lib/api/route-helpers';
import { clearUserSession } from '@/lib/auth/session';

export async function POST() {
  await clearUserSession();
  return okJson({ success: true });
}
