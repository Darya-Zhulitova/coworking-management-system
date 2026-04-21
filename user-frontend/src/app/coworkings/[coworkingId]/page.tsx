import { CoworkingPageContent } from '@/components/features/coworkings/coworking-page-content';
import { requireUserSession } from '@/lib/auth/session';

export default async function CoworkingPage({ params }: { params: Promise<{ coworkingId: string }> }) {
  await requireUserSession();
  const { coworkingId } = await params;
  return <CoworkingPageContent coworkingId={Number(coworkingId)}/>;
}
