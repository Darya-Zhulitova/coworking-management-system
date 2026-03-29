type CoworkingPageProps = {
  params: Promise<{
    coworkingId: string;
  }>;
};

export default async function CoworkingPage({params}: CoworkingPageProps) {
  const {coworkingId} = await params;

  return (
    <div>
      <div className="panel">
        <h2>Coworking</h2>
        <p>Текущий coworkingId: <strong>{coworkingId}</strong></p>
      </div>
    </div>
  );
}
