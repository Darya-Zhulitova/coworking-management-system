import {RequestsPage} from '@/components/features/requests/requests-page';

export default async function RequestsRoute({params}: { params: Promise<{ coworkingId: string }> }) {
  const {coworkingId} = await params;
  return <RequestsPage coworkingId={Number(coworkingId)}/>;
}
