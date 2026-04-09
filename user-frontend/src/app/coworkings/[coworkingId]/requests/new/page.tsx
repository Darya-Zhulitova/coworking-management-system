import {RequestNewPage} from '@/components/features/requests/request-new-page';

export default async function RequestNewRoute({params}: { params: Promise<{ coworkingId: string }> }) {
  const {coworkingId} = await params;
  return <RequestNewPage coworkingId={Number(coworkingId)}/>;
}
