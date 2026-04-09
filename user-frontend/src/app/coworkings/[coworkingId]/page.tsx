import {CoworkingPageContent} from '@/components/features/coworkings/coworking-page-content';

export default async function CoworkingPage({params}: { params: Promise<{ coworkingId: string }> }) {
  const {coworkingId} = await params;
  return <CoworkingPageContent coworkingId={Number(coworkingId)}/>;
}
