type CoworkingPageProps = {
  params: Promise<{
    coworkingId: string;
  }>;
};

export default async function CoworkingPage({params}: CoworkingPageProps) {
  const {coworkingId} = await params;

  return (
    <section className="card shadow-sm border-0">
      <div className="card-body p-4">
        <h2 className="h4 mb-3">Coworking</h2>
        <p className="mb-0">
          Текущий coworkingId: <strong>{coworkingId}</strong>
        </p>
      </div>
    </section>
  );
}
