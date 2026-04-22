import { redirect } from 'next/navigation';

export default async function CoworkingPlacesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  redirect(`/coworkings/${id}/settings`);
}
